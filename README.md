** NOVO **: SVG spriting podrška
=============================================

SVG spriting podrška

- se sastoji od niza novih startup parametara i css comment naredbi koji omogućavaju kreiranje svg sprite-ova i u skladu s tim ova dokumentacija će
  obuhvatiti 1) nove startup parametre i 2) nove css comment naredbe. Za listu starih startup komandi i css comment naredbi pogledati https://csssprites.org/

**1)** **Novi startup parametri**
   
   `--sprite-dir-path ../../../img/white` - označava relativnu putanju gdje će se svg sprite smještati ukoliko ne postoji svg-sprite-image naredba u css fajlu (u ovom slučaju ako imamo fajl `../res/css/themes/white/a.css` koji nema `svg-sprite-image` komentar, svg sprite će biti kreiran u `../res/img/white/` direktorijumu i imaće ime `a{$spriteFileSuffix}.svg`)
    
   `--ignore-dir-paths lib,lib2` - ignorisaće se svi css fajlovi sa `--root-dir-path/lib` i `--root-dir-path/lib2` putanja (ovo je korisno kada imamo css fajlove nekih biblioteka (tipa d3, highcharts...) koje ne želimo procesirati prilikom kreiranja novih sprite-ova)
    
   `--sprite-file-suffix -sprite-id-1` - sprite-ovi će dobijati proslijedjeni sufiks (Primjer: svg-ovi iz a.css bi završili u a-sprite-id-1.svg u slučaju da koristimo konfig iz ovog primjera (ili ako ovo iskažemo u opštem slučaju ime sprite-a  = `${cssFileName}${fileSuffix}.svg`) (trenutno `--sprite-file-suffix` radi samo sa svg sprite-ovima)
  

**2)** **Nove css comment naredbe**
   
   Primjer a.css:

   Content fajla:
```css
/* svg-sprite-image: url('sprite.svg'); */ <--- /* opcionalan parametar (ukoliko ne postoji čita se vrijednost --sprite-dir-path startup parametra i sprite se kreira na osnovu njega)
                                                    Ukoliko svg-sprite-image postoji u css fajlu, svg sprite će biti sačuvan na lokaciji koja je odredjena u vrijednosti url parametra
                                                    (u ovom slučaju na %PUTANJA_DO_CSS_FAJLA%/sprite.svg)*/

   .ikonica-1 {
     background-image: url('/ui/common/res/img/def/icon.svg'); /* exclude-from-sprite: true */ <--- /* icon.svg ikonica neće završiti u sprite-u */
   }

   .ikonica-2 {
     mask-image: url('/ui/common/res/img/def/icon2.svg');
     -webkit-mask-image: url('/ui/common/res/img/def/icon2.svg'); <--- /* icon2 će završiti u sprite.svg sprite-u */
   }

   .ikonica-3 {
     background: url('/ui/common/res/img/def/icon3.svg'); <--- /* icon3 će završiti u sprite.svg sprite-u */
   }

   .ikonica-4 {
     background-image: url('/ui/common/res/img/def/icon3.svg'); <--- /* icon4 će završiti u sprite.svg sprite-u */
   }
```


Par napomena o radu smartsprites-a sa svg podrškom

- Po defaultu se svi svg fajlovi dodaju u sprite i ukoliko se želi izbjeći ovo ponašanje potrebno je navesti 
  `/* exclude-from-sprite: true */` na kraju css rule-a koji referencira upravo taj svg fajl
- smartsprites će pokupiti sve svg definicije unutar css-a bez obzira da li se vežu za `background` ili `background-image` ili `(-webkit-)mask-image` rule-ove




Primjer izvrsavanja: 


       smartsprites --root-dir-path test/real-world-example
       --ignore-dir-paths lib,build/lib,etc/css 
       --sprite-dir-path ../../../img/white
       --sprite-file-suffix -sprite-id-1
       
       
Komanda za dobijanje produkcijskog jar-a

       mvn package -Prelease

For OSSRH staging upload:

       mvn clean package -Prelease,sonatype
  
