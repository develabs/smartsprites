** NOVO **: SVG spriting podrška
=============================================

SVG spriting podrška

- se sastoji od niza novih startup parametara i css comment naredbi koji omogućavaju kreiranje svg sprite-ova i u skladu s tim ova dokumentacija će
  obuhvatiti 1) nove startup parametre i 2) nove css comment naredbe. Za listu starih startup komandi i css comment naredbi pogledati https://csssprites.org/

**1)** **Novi startup parametri**
   
   `--svg-sprite-rel-location ../../../img/white` - označava relativnu putanju gdje će se svg sprite smještati ukoliko ne postoji svg-sprite-image naredba u css fajlu (u ovom slučaju ako imamo fajl `../res/css/themes/white/a.css` koji nema `svg-sprite-image` komentar, svg sprite će biti kreiran u `../res/img/white/` direktorijumu i imaće ime `a-sprite.svg`)
    
   `--ignore-dirs lib,lib2` - ignorisaće se svi css fajlovi iz `--root-dir-path/lib` i `--root-dir-path/lib2` foldera (ovo je korisno kada imamo css fajlove nekih biblioteka (tipa d3, highcharts...) koje ne želimo procesirati prilikom kreiranja novih sprite-ova)


**2)** **Nove css comment naredbe**
   
   Primjer a.css:

   Content fajla:
```css
/* svg-sprite-image: url('sprite.svg'); */ <--- /* opcionalan parametar (ukoliko ne postoji čita se vrijednost --svg-sprite-rel-location startup parametra i sprite se kreira na osnovu njega)
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

- Po defaultu se svi svg fajlovi dodaju u sprite i ukoliko se želi izbjeći dodavanje svg fajla u sprite potrebno je navesti 
  `/* exclude-from-sprite: true */` na kraju css rule-a koji referencira upravo taj svg fajl
- smartsprites će pokupiti sve svg definicije unutar css-a bez obzira da li se vežu za `background` ili `background-image` ili `(-webkit-)mask-image` rule-ove i nije potrebno navoditi nikakve naredbe da svg udje u sprite,
  jer će po defaultu završiti u sprite-u




Primjer izvrsavanja: 


       smartsprites --root-dir-path test/real-world-example
       --ignore-dirs lib,build/lib,etc/css 
       --svg-sprite-rel-location ../../../img/white
       
       
Komanda za dobijanje produkcijskog jar-a

       mvn package -Prelease

For OSSRH staging upload:
mvn clean package -Prelease,sonatype
