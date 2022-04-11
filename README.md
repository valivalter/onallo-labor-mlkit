# Önálló labor

### Madáretető Tensor Flow alapú automatikus felismeréssel és fotózással

A téma célja az Android MLKit és a TensorFLow megismerése egy példaalkalmazáson keresztül.
Az alkalmazás célja, hogy egy (a fejlesztés során mobiltelefonnal szimulált) webkamerás Android egységgel felszerelt madáretető készítsünk, amely felismeri, ha madár szállt rá, azonosítja, és fotókat készít róla.

## Haladási napló

### 2. hét

Megnéztem és elolvastam a kapott ML Kitet ismertető anyagot, átnéztem a példaprogram forráskódját, majd telefonon is lefuttattam. Itt-ott végeztem néhány kisebb módosítást (például, hogy az elülső kamerát használja az alkalmazás a hátsó helyett a könnyebb tesztelhetőség érdekében), valamint kibővítettem az alkalmazást azzal, hogy TTS motor segítségével ejtse ki annak a testrésznek a nevét, amihez a felhasználó hozzáér a bal mutatóujjával - például jobb váll, ball váll, orr, száj jobb széle, stb. Ez a funkció egyelőre szeszélyesen működik, javításokra szorul még.

### 3. hét

Kezdésképpen letöltöttem és futtattam az [ML Kit Vision Quickstart Sample App](https://github.com/googlesamples/mlkit/tree/master/android/vision-quickstart) és az [ML Kit Vision Showcase App with Material Design](https://github.com/googlesamples/mlkit/tree/master/android/material-showcase) alkalmazást, majd tanulmányoztam főleg az előbbi forráskódját, de később majd az utóbbiéra is szeretnék időt szánni.

Még az előző hétről maradt meg az a problémám, hogy a *PreviewView* sehogyan sem jelent meg megfelelően, csak a képernyő felső felén, összenyomottan - emulátoron és igazi telefonon kipróbálva is ezt tapasztaltam. Ennek a megoldásával a vártnál több időt töltöttem, végül kiderült, hogy a probléma oka a manifest fájlban rejtőzött, ki kellett törölnöm a ```android:hardwareAccelerated="false"``` szöveget tartalmazó sort.

Ezután kiegészítettem az alkalmazást objektumfelismeréshez és -követéshez szükséges kódrészletekkel és osztályokkal. Ezek a telefon kameráját használva felismernek bizonyos objektumokat, majd különböző színű téglalapokkal jelzik az objektumok befoglaló téglalapjait, az ugyanahhoz az objektumhoz tartozó befoglaló doboz színe nem változik a kamera mozgatása során, ehhez szükség volt az ML Kit objektumkövető képességére is.

A következő kiegészítés az ML Kitnek egy másik felhasználási lehetőségét tesztelte: letöltöttem TensorFlow Hubról egy TensorFlow Lite modellt, ami képes felismerni ismert európai látványosságokat, épületeket. Ezt beépítettem az alkalmazásba, így az folyamatosan kiírta a képernyő közepére, hogy éppen milyen látványosságot vél leginkább felismerni a kamera képén. Ezt sikeresen teszteltem is azzal, hogy a monitoromon megjelenítettem többek között a Buckingham-palotát és a Trevi-kutat, ezeket mind sikeresen felismerte az általam használt előre betanított hálózat.

### 4-5. hét

A célom egy madarakat beazonosító neurális háló betanítása volt TensorFlow Lite Model Maker segítségével. Ehhez elolvastam a [dokumentációját](https://www.tensorflow.org/lite/guide/model_maker), leginkább a képosztályozásról szóló fejezeteket. Ezután kerestem Kaggle-ön egy [megfelelő képgyűjteményt](https://www.kaggle.com/datasets/gpiosenka/100-bird-species) a tanításhoz. A tanítást Google Colabbal végeztem el: először közvetlenül próbáltam meg feltölteni a képeket, viszont ez túlságosan lassúnak bizonyult, ráadásul a futtatókörnyezettel meglévő kapcsolat is megszakadt többször egymás után, így másik megoldást kellett keresnem, itt jött be a képbe a Kaggle API-ja, ez már gyorsan és kifogástalanul működött. A teljes tanítást tartalmazza a *BirdClassification.ipynb* nevű fájl, ebben látható, hogy 90%-os pontosságot ért el a mesterséges intelligencia. Alkalmazásban kipróbálva egy fizikai készüléken még nem működik, ez feltételezhetően a még nem megfelelően beállított metaadatok miatt van.

### 6. hét

Kiderült, hogy csak azért nem működött előző héten az alkalmazásban a madarak beazonosítása, mert túl magasra volt állítva a magabiztossági küszöb (*confidence threshold*), ezt lejjebbvéve már működött is a program. Ezután átírtam az alkalmazást, hogy ezt a neurális hálót ne képosztályozáshoz használja, hanem objektumfelismeréshez és -követéshez, valamint hogy helyesen jelenítse meg a felismert objektumok körülvevő téglalapjait, és ki is írja, hogy milyen madárnak véli felismerni az adott képrészletet. Tesztelésképpen egymás mellé bevágtam 16-szor ugyanazt a képet egy cifra récéről, hogy megtudjam azt, hogy hány objektumot tud egyszerre detektálni az alkalmazás.

<p align="center">
  <img src="baikal-teals.jpg" width=50% >
</p>

5-nél többet sehogyan sem tud felismerni az alkalmazás, ez a szám az ML Kit korlátja.

Következő lépésként készítettem közel 250 képet az egyik otthoni sakk-készletemről, feltöltöttem Kaggle-re, majd megpróbáltam betanítani egy neurális hálót a bábuk felismerésére (*ChessPieceClassification.ipynb*), de ennek épphogy 50% fölött lett csak a pontossága sajnos, habár legalább a bábuk színét minden egyes esetben helyesen ítélte meg. Az alkalmazásba beépítve a kiexportált modellt sajnos rosszabb eredmények születtek, mint amiket vártam volna a Colabon tapasztaltak után, de vannak ötleteim a hatékonyság növelésére.

### 8. hét

A TensorFlow Lite Model Maker dokumentációját olvasva láttam, hogy objektumok felismerésére és követésére máshogyan ajánlott modellt betanítani, mint egyszerű képosztályozáshoz, de én eddig csak az utóbbi módszert alkalmaztam a betanításnál, amihez a saját datasetemet használtam, ezért az előbbi módot is ki akartam próbálni, ahhoz viszont egy CSV fájlban meg kell adni minden egyes képhez tartozóan a rajta lévő objektumokat és a befoglaló téglalapjaiknak a koordinátáit, így inkább megnéztem milyen publikusan elérhető képgyűjtemények léteznek már a sakkbábu-felismerés problémájára.

[Találtam is egyet](https://public.roboflow.com/object-detection/chess-full), amihez az előbb említett módon megvoltak a befoglaló téglalapoknak a koordinátái is, habár a formátum nem volt megfelelő, ezért a CSV-t manuálisan is kellett szerkesztenem, hogy a Model Maker elfogadja (*annotations.csv*). Miután ez sikerült, a *ChessPieceDetector.ipynb*-ban látható módon betanítottam a hálózatot. Ezután tesztelni akartam fizikai készüléken a modellt, viszont ekkor derült ki, hogy sajnos félreértettem az ML Kit képességeit, mert a modellre hibát dobott, mégpedig azért, mert az ML Kites objektumdetekció csak saját képosztályozó neurális háló használatát támogatja, teljes objektumdetektálást és még osztályozást is végző modellt pedig nem.

Így ezek után egy [harmadik datasethez](https://www.dropbox.com/s/618l4ddoykotmru/Chess%20ID%20Public%20Data.zip) fordultam, amiben sajnos DS_STORE fájlok is voltak, ami egy kis kellemetlenséget okozott a Colab használatakor, de ezen hamar túllendültem. Ismét csak egy képosztályozó modellt tanítottam be ezekkel az újonnan talált képekkel (*ChessPieceClassificationPublicImages.ipynb*), a kapott pontosság pedig lényegesen jobb lett, mint amit a saját képeimet használva sikerült elérni.

(Ezután tettem egy kis kitérőt: egy olyan GitHubon talált Python kódot próbáltam ki, ami tetszőleges képről automatikusan kivágja és perspektivikusan átméretezi a képen lévő sakktáblát; ebben a kódban, és néhány egyéb cikkben láttam, hogy mi a legtöbbet alkalmazott algoritmus egy sakktábla felismerérése egy képen, ezt majd később fel tudom használni, amikor ehhez a részhez érek az alkalmazásomban.)

Végül elkezdtem írni egy új alkalmazást Chess Analyzer néven, ez futtatáskor elindítja a beépített kamera alkalmazást, majd miután fotóztunk egyet (felülről kell lefotózni a sakktáblát úgy, hogy annak szélessége megegyezzen a képernyő szélességével, valamint a tábla felső fele a képernyő tetejénél legyen), a friss képet feldarabolja a sakktábla mezői mentén, majd a mezőket egyesével elküldi feldolgozásra a képosztályozó neurális hálónak, a háló kimeneteiből pedig felépít egy sakktábla objektumot.