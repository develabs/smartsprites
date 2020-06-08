package org.carrot2.labs.smartsprites;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.carrot2.labs.smartsprites.message.Message.MessageType;
import org.carrot2.labs.smartsprites.message.MessageLog;
import org.carrot2.labs.smartsprites.resource.ResourceHandler;
import org.carrot2.labs.smartsprites.svgmodel.SvgReplacementInfo;
import org.carrot2.util.FileUtils;
import org.carrot2.util.PathUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class SvgSpriteBuilder {

	public static final String CSS_DEFINITION_REGEX = "\\{([^\\{\\}]|\\n|\\r|)*\\}";
	public static final String CSS_COMMENT_REGEX = "\\/\\*[\\s\\S]*?\\*\\/";

	public static final String DEF_SVG_SPRITE_IMAGE_REGEX = "svg-sprite-image\\s*:[^;]*;";
	public static final String EXCLUDE_FROM_SPRITE_RULE_REGEX = "exclude-from-sprite\\s*:\\s*true";

	public static final String SVG_URL_REGEX = "\\s*url.*\\.svg['\")]";
	public static final String BG_IMAGE_SVG_REGEX = "background-image\\s*:" + SVG_URL_REGEX + ".*";
	public static final String BG_SVG_REGEX = "background\\s*:" + SVG_URL_REGEX + ".*";
	public static final String MASK_IMAGE_SVG_REGEX = "mask-image\\s*:" + SVG_URL_REGEX + ".*";
	public static final String WEBKIT_MASK_IMAGE_SVG_REGEX = "-webkit-mask-image\\s*:" + SVG_URL_REGEX + ".*";
	public static final String ANY_SVG_IMAGE_REGEX = "(background(-image)?|(-webkit-)?mask-image)\\s*:" + SVG_URL_REGEX + ".*";

	private SmartSpritesParameters parameters;
	private MessageLog messageLog;
	private ResourceHandler resourceHandler;


	public SvgSpriteBuilder(SmartSpritesParameters parameters, MessageLog messageLog,
		ResourceHandler resourceHandler)
	{
		this.messageLog = messageLog;
		this.parameters = parameters;
		this.resourceHandler = resourceHandler;
	}


	private List<String> getAllMatches(String regexPattern, String searchedString) {

		List<String> allMatches = new ArrayList<String>();
		Matcher m = Pattern.compile(regexPattern)
			.matcher(searchedString);
		while (m.find()) {
			allMatches.add(m.group());
		}
		return allMatches;
	}

	/**
	 * Eg: For some-key:url('s2.svg'); it will return s2.svg
	 */
	private String getCssUrlContent(String input) {
		return input.split("url\\(")[1].split("\\)")[0].replace("'","").replace("\"","");
	}

	/**
	 * Eg: For sprite-ref: a; it will return a
	 */
	private String getCssRuleValue(String input) {
		return input.split(":")[1].split(";")[0].trim();
	}

	private String getCssCommentAtTheEndOfARule(String singleCssDefinition) {
		String result = "";

		List<String> matches = getAllMatches("\\/\\*.*", singleCssDefinition);
		for (int i = 0; i < matches.size(); i++) {
			result = result.concat(matches.get(i));
		}

		return result;
	}

	/**
	 *
	 * @return true if css comment contains /**  exclude-from-sprite: true ...
	 */
	private boolean excludeSvgFromSprite(String cssComment) {
		return Pattern.compile(EXCLUDE_FROM_SPRITE_RULE_REGEX).matcher(cssComment).find();
	}

	/**
	 *
	 * @return true if root <svg/> element has <path/> or multiple <g/> elements as first children
	 */
	private boolean incorrectSvgSpriteSource (Element sourceSvgRoot, List<String> sourceSvgFileNames, int i) {
		// Check if root has multiple g elements or any path element
		int gElementsCount = 0;
		for (Node n = sourceSvgRoot.getFirstChild(); n != null; n = n.getNextSibling()) {
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				Element childElement = (Element) n;

				if (childElement.getTagName().toLowerCase().equals("path")) {
					messageLog.info(MessageType.GENERIC, "SVG file ".concat(sourceSvgFileNames.get(i)).concat(" <svg> contains <path> element as <g> element sibling (both as svg element first children, this svg definition won't end up in svg sprite)"));
					return true;
				}
				if (childElement.getTagName().toLowerCase().equals("g")) {
					++gElementsCount;
				}
				if (gElementsCount > 1) {
					messageLog.info(MessageType.GENERIC, "SVG file ".concat(sourceSvgFileNames.get(i)).concat(" contains multiple <g> elements as first <svg> children (this svg definition won't end up in svg sprite)"));
					return true;
				}
			}

		}
		return false;
	}

	private void writeToSprite(String spriteFileName, List<SvgReplacementInfo> replacements)
		throws SAXException, IOException, ParserConfigurationException, TransformerException {

		if (replacements.size() < 1) {
			return;
		}


		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = factory.newDocumentBuilder();

		List<Element> sourceSvgRootElements = new ArrayList<>();

		List<File> sourceSvgFiles = new ArrayList<File>();
		List<String> sourceSvgFileNames = new ArrayList<>();

		for (SvgReplacementInfo replacement: replacements) {

			if (!sourceSvgFileNames.contains(replacement.getSourceSvgFile())) {

				File svgSourceFile = new File(replacement.getSourceSvgFile());

				if (svgSourceFile.exists()) {
					sourceSvgFileNames.add(replacement.getSourceSvgFile());
					sourceSvgFiles.add(svgSourceFile);
				} else {
					messageLog.warning(MessageType.GENERIC,
						svgSourceFile + " does not exist");
				}
			}


		}

		for (File f: sourceSvgFiles) {
			Document doc = dBuilder.parse(f);
			sourceSvgRootElements.add(doc.getDocumentElement());
		}

		// Create sprite file
		Document spriteDoc = dBuilder.newDocument();

		Element root = spriteDoc.createElement("svg");
		root.setAttribute("xmlns","http://www.w3.org/2000/svg");
		root.setAttribute("xmlns:xlink","http://www.w3.org/1999/xlink");
		spriteDoc.appendChild(root);

		Element defs = spriteDoc.createElement("defs");
		Element style = spriteDoc.createElement("style");

		style.setTextContent("\n.icon {\n"
			+ "\t\t\tdisplay : none;\n"
			+ "\t\t}\n"
			+ "\t\t\t.icon:target {\n"
			+ "\t\t\tdisplay: inline;\n"
			+ "\t\t}\n");

		defs.appendChild(style);
		root.appendChild(defs);

		for (int i = 0; i < sourceSvgRootElements.size(); i++) {
			Element sourceSvgRoot = sourceSvgRootElements.get(i);


			if (incorrectSvgSpriteSource(sourceSvgRoot,sourceSvgFileNames,i)) {
				continue;
			}


			Node firstG = sourceSvgRoot.getElementsByTagName("g").item(0);

			if (firstG.getNodeType() == Node.ELEMENT_NODE) {
				Element e = (Element) firstG;

				e.setAttribute("id", String.valueOf(i));
				e.setAttribute("class", "icon");

				String normalizedDocroot = parameters.getDocumentRootDir().replace("\\","/");
				String normalizedSpriteFileName = spriteFileName.replace("\\", "/");

				String replaceWith = "/" + PathUtils.getRelativeFilePath(normalizedDocroot, normalizedSpriteFileName) + "#" + i;
				replaceWith = replaceWith.replace("\\","/"); // normalize replacement paths
				String sourceSvgFileName = sourceSvgFileNames.get(i);


				for (SvgReplacementInfo replacement : replacements) {
					if (replacement.getSourceSvgFile().equals(sourceSvgFileName)) {
						replacement.setReplaceWith(replaceWith);
					}
				}



			}
			root.appendChild(spriteDoc.importNode(sourceSvgRoot,true));
		}

		/// WRITE TO SVG SPRITE FILE
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transf = transformerFactory.newTransformer();

		DOMSource source = new DOMSource(spriteDoc);
		File spriteFile = new File(spriteFileName);
		StreamResult file = new StreamResult(spriteFile);
		transf.transform(source, file);

	}


	/**
	 *
	 * eg: "C:/Develabs/smartsprites-master/test/real-world-example/testSprite.svg" : [lista SvgReplacementInfo objekata]
	 */

	private void createSpriteImage(String svgSpriteFileName, List<SvgReplacementInfo> replacements)
		throws Exception {

		if (replacements.size() < 1) {
			return;
		}

		File targetFile = new File(svgSpriteFileName);
		File parent = targetFile.getParentFile();
		if (!parent.exists() && !parent.mkdirs()) {
			throw new IllegalStateException("Couldn't create dir: " + parent);
		}
		writeToSprite(svgSpriteFileName, replacements);

	}


	private String getPathConnector(String firstPart, String secondPart) {
		return (firstPart.endsWith("/") || secondPart.startsWith("/")) ? "" : "/";
	}

	private String getConnectedPath(String firstPart, String secondPart) {

		String normalized1 = firstPart.replace('\\','/');
		String normalized2 = secondPart.replace('\\','/');

		return normalized1.concat(getPathConnector(normalized1, normalized2)).concat(normalized2);
	}

	private void populateSvgSpritesMultimap(String cssFileName, Multimap<String, SvgReplacementInfo> svgSpritesMultimap) throws Exception{

		String spriteEnding = parameters.getSpriteFileSuffix().concat(".svg");
		String normalizedCssFileName = cssFileName.replace("\\","/");

		String[] paths = normalizedCssFileName.split("/");
		String defSpriteName = paths[paths.length-1].split("\\.css")[0].concat(spriteEnding);

		String thisCssFirstParentPath = "";

		for (int i = 0; i < paths.length - 1; i++) {
			thisCssFirstParentPath = thisCssFirstParentPath.concat("/").concat(paths[i]);
		}

		String content = new String(Files.readAllBytes(Paths.get(cssFileName)));


		// Get all smartsprites svg image definitions
		List<String> defSvgSpriteImageDefinition = getAllMatches(DEF_SVG_SPRITE_IMAGE_REGEX, content);

		if (defSvgSpriteImageDefinition.size() > 1 ) {
			messageLog.warning(MessageType.MULTIPLE_SVG_IMAGE_RULES_FOUND);
		}

		String defSvgSpriteImageLoc = null;

		boolean hasDefSvgSpriteImageDefinition = defSvgSpriteImageDefinition.size() > 0;

		if (hasDefSvgSpriteImageDefinition) {
			defSvgSpriteImageLoc = getCssUrlContent(defSvgSpriteImageDefinition.get(0)); // SPRITE REL LOCATION from comment inside this css file
		}


		// Get all svg image rule values (background(-image),(-webkit-)mask-image: url('...svg') /* With css comments */)

		List<String> svgImageRules = getAllMatches(ANY_SVG_IMAGE_REGEX, content);


		String finalLocationForSvgSprite = (hasDefSvgSpriteImageDefinition) ?
			getConnectedPath(parameters.getDocumentRootDir(), defSvgSpriteImageLoc) :
			getConnectedPath(getConnectedPath(thisCssFirstParentPath, parameters.getSpriteDirPath()), defSpriteName); // use startup param svg sprite location or location from css comment rule

		finalLocationForSvgSprite = FileUtils.canonicalize(finalLocationForSvgSprite, "/");

		for (int i = 0; i < svgImageRules.size(); i++) {

			String svgImageRule = svgImageRules.get(i);

			String ruleComment = getCssCommentAtTheEndOfARule(svgImageRule);
			boolean excludeSvgFromSprite = excludeSvgFromSprite(ruleComment);


			if (!excludeSvgFromSprite) {

				String imgUrlRuleValue = getCssUrlContent(svgImageRule);
				boolean hasImportantTag = Pattern.compile("!important\\s*;").matcher(svgImageRule).find();


				String realImagePath = resourceHandler.getResourcePath(cssFileName, imgUrlRuleValue);
				realImagePath = realImagePath.replace("\\","/");
				realImagePath = FileUtils.canonicalize(realImagePath, "/");

				SvgReplacementInfo svgReplInfo = new SvgReplacementInfo(imgUrlRuleValue,null, hasImportantTag, realImagePath, cssFileName);
				svgSpritesMultimap.put(finalLocationForSvgSprite, svgReplInfo);
			}
		}
	}


	public void buildSprites(Collection<String> filePaths) throws Exception {

		List<String> allCssFilePaths = new ArrayList<>(filePaths);
		Multimap<String, SvgReplacementInfo> svgSpritesMultimap = LinkedListMultimap.create();


		// Read all forwarded css files and populate <svgSpriteFileName,svgReplacementInfo> multimap
		for (String cssFileName: allCssFilePaths) {
			populateSvgSpritesMultimap(cssFileName, svgSpritesMultimap);
		}

		// Generate .svg sprite sheet and populate replacement strings inside SvgReplacementInfo objects
		for (String svgSpriteFileName: svgSpritesMultimap.keySet()) {
			List<SvgReplacementInfo> replacements = new ArrayList<>(svgSpritesMultimap.get(svgSpriteFileName));
			createSpriteImage(svgSpriteFileName, replacements);
		}

		// Rewrite .css files
		messageLog.info(MessageType.GENERIC, "=============== svg sprite generation");

		Multimap<String, SvgReplacementInfo> replacementMultimap = LinkedListMultimap.create();

		for (String svgSpriteFileName: svgSpritesMultimap.keySet()) {
			List<SvgReplacementInfo> replacements = new ArrayList<>(svgSpritesMultimap.get(svgSpriteFileName));

			for (SvgReplacementInfo replacement: replacements) {
				//messageLog.info(MessageType.GENERIC, "Reading svg replacement info: " + replacement.toString());
				replacementMultimap.put(replacement.getSourceCssFile(), replacement);
			}
		}

		for (String cssFileName: replacementMultimap.keySet()) {
			List<SvgReplacementInfo> replacements = new ArrayList<>(replacementMultimap.get(cssFileName));

			if (replacements.size() < 1) {
				continue;
			}

			String outputCssFileName = cssFileName.split("\\.css")[0].concat(SmartSpritesParameters.DEFAULT_CSS_FILE_SUFFIX).concat(".css");
			String cssContent = new String(Files.readAllBytes(Paths.get(cssFileName)));

			if (cssContent.trim().isEmpty())
			{
				messageLog.warning(MessageType.GENERIC, "Empty css file found: ".concat(cssFileName));
				continue;
			}


			for (SvgReplacementInfo replacement: replacements) {

				if (replacement.getFindWhat() == null || replacement.getReplaceWith() == null) {
					messageLog.warning(MessageType.GENERIC, "Empty replacement found: ".concat(replacement.toString()));
					continue;
				}

				cssContent = cssContent.replace(replacement.getFindWhat(), replacement.getReplaceWith());
			}
			BufferedWriter writer = new BufferedWriter(new FileWriter(outputCssFileName));
			writer.write(cssContent);

			writer.close();
		}

	}
}
