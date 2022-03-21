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

A [TensorFlow Lite Model Maker](https://www.tensorflow.org/lite/guide/model_maker) dokumentációját olvastam, leginkább a képosztályozásról szóló fejezeteket. Ezek felhasználásával készült el a *BirdClassification.ipynb* nevű fájl, ami egy Kaggle-ön talált képgyűjteménnyel tanít be egy neurális hálót madarak felismerésére. Alkalmazásban kipróbálva egy fizikai készüléken még nem működik, ez feltételezhetően a még nem megfelelően beállított metaadatoknak köszönhető.