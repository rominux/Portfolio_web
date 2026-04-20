class CTPSaE1v2 extends Program {

final int RED    = 0;
final int GREEN  = 1;
final int BLUE   = 2;

    // Q1 : Définition de la fonction colorCode (4 pt)

    int colorCode(String[] palette, String color){
        int compteur = -1;
        boolean trouve = false;
        while (compteur<length(palette)-1 && !trouve){
            compteur = compteur + 1;
            if (equals(palette[compteur],color)){
                trouve = true;
            }
        }
        if (!trouve){
            compteur = -1;
        }
        return compteur;
    }

    void test_colorCode() {
    final String[] PALETTE = {
            "255000000", // rouge
            "000255000", // vert
            "000000255", // bleu
            "000000000", // noir
            "255255255"  // blanc
    };
    assertEquals( 0, colorCode(PALETTE, "255000000"));
    assertEquals( 4, colorCode(PALETTE, "255255255"));
    assertEquals(-1, colorCode(PALETTE, "032064032"));
}


            // Q2 : Définition de la fonction numberOfPixels (2 pt)

    int numberOfPixels(String image){
        return length(image)/9;
    }

    void test_numberOfPixels() {
    final String IMAGE_2x2 = "255255255"+"000255000"+
                                 "255255255"+"000255000";
        assertEquals(4, numberOfPixels(IMAGE_2x2));

        final String IMAGE_3x3 = "255000000"+"000255000"+"000000255"+
                                 "000000000"+"255000000"+"000255000"+
                                 "255255255"+"000000000"+"255000000";
        assertEquals(9, numberOfPixels(IMAGE_3x3));
    }


            // Q3 : int[] convertOldToNew(String image, String[] palette) (4 pts)

    int[] convertOldToNew(String image, String[] palette){
        int[] res = new int[numberOfPixels(image)];
        for (int i = 0; i<numberOfPixels(image); i= i +1){
            res[i] = colorCode(palette,substring(image,i*9,i*9+9));
        }
        return res;
    }

    void test_convertOldToNew() {
    final String IMAGE_2x2 = "255255255"+"000255000"+
                                 "255255255"+"000255000";
        final String[] PALETTE = {
            "255000000", // rouge
            "000255000", // vert
            "000000255", // bleu
            "000000000", // noir
            "255255255"  // blanc
        };
        assertArrayEquals(new int[]{4, 1, 4, 1}, 
                    convertOldToNew(IMAGE_2x2, PALETTE));
        
        final String IMAGE_3x3 = "255000000"+"000255000"+"000000255"+
                                 "000000000"+"255000000"+"000255000"+
                                 "255255255"+"000000000"+"255000000";
        assertArrayEquals(new int[]{0, 1, 2, 3, 0, 1, 4, 3, 0}, 
                    convertOldToNew(IMAGE_3x3, PALETTE));
    }


            // Q4 : String convertNewToOld(int[] image, String[] palette) (4 pts)

    String convertNewToOld(int[] image, String[] palette){
        String res = "";
        for (int i = 0; i<length(image); i=i + 1){
            res = res + palette[image[i]];
        }
        return res;
    }

    void test_convertNewToOld() {
    final String[] PALETTE = {
            "255000000", // rouge
            "000255000", // vert
            "000000255", // bleu
            "000000000", // noir
            "255255255"  // blanc
        };
        final String IMAGE_2x2 = "255255255"+"000255000"+
                                 "255255255"+"000255000";
        assertEquals(IMAGE_2x2, 
                    convertNewToOld(new int[]{4, 1, 4, 1}, PALETTE));
        
        final String IMAGE_3x3 = "255000000"+"000255000"+"000000255"+
                                 "000000000"+"255000000"+"000255000"+
                                 "255255255"+"000000000"+"255000000";
        assertEquals(IMAGE_3x3, 
                    convertNewToOld(new int[]{0, 1, 2, 3, 0, 1, 4, 3, 0}, PALETTE));
    }


            // Q5 : String[] shift(String[] palette, boolean right) (3 pts)

    String[] shift(String[] palette, boolean right){
        String[] res = new String[length(palette)];
        if (right){
            for (int i = 0; i<length(palette);i=i+1){
                if (i+1==length(palette)){
                    res[0] = palette[length(palette)-1];
                } else {
                    res[i+1] = palette[i];
                }
            }
        } else {
            for (int i = 0; i<length(palette);i=i+1){
                if (i!=0){
                    res[i-1] = palette[i];
                } else {
                    res[length(palette)-1] = palette[0];
                }
            }
        }
        return res;
    }

    void test_shift() {
    final String[] PALETTE = new String[]{"255000000", "000255000", "000000255"};
    assertArrayEquals(new String[]{"000000255", "255000000", "000255000"}, 
                      shift(PALETTE, true));
    assertArrayEquals(new String[]{"000255000", "000000255", "255000000"}, 
                      shift(PALETTE, false));
    }


            // Q6 : void show(int[] image, String[] palette) (3 pts)
    
    int charToInt(char digit) {
        return (int) (digit - '0');
    }
    int primaryColorToInt(String primaryColor) {
        return charToInt(charAt(primaryColor, 0)) * 100 +
               charToInt(charAt(primaryColor, 1)) *  10 +
               charToInt(charAt(primaryColor, 2));
    }
    int primaryColorIndex(int primaryColor) {
        return primaryColor * 3;
    }
    int get(String color, int primaryColor) {
        int indiceDebut = primaryColorIndex(primaryColor);
        return primaryColorToInt(substring(color, indiceDebut, indiceDebut + 3));
    }
    int size(int[] image) {
        return sqrt(length(image));
    }

    void show(int[] image, String[] palette){
        int imageSize = size(image);
        for (int ligne = 0; ligne < imageSize; ligne = ligne + 1) {
            for (int colonne = 0; colonne < imageSize; colonne = colonne + 1) {
                print(rgb(
                    get(palette[image[ligne*imageSize + colonne]],RED),
                    get(palette[image[ligne*imageSize + colonne]],GREEN),
                    get(palette[image[ligne*imageSize + colonne]],BLUE),
                    false) + ' ' + RESET);
            }
            print("\n");
        }
    }


            void algorithm() {
                final String IMAGE_3x3 = "255000000"+"000255000"+"000000255"+
                                        "000000000"+"255000000"+"000255000"+
                                        "255255255"+"000000000"+"255000000";
                final String[] PALETTE = {
                    "255000000", // rouge
                    "000255000", // vert
                    "000000255", // bleu
                    "000000000", // noir
                    "255255255"  // blanc
                };
                show(convertOldToNew(IMAGE_3x3, PALETTE), PALETTE);
            }

}
