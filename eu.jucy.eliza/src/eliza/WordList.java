package eliza;

import java.util.ArrayList;


/**
 *  Eliza word list.
 */
@SuppressWarnings("serial")
public class WordList extends ArrayList<String> {

//    /**
//     *  Add another word to the list.
//     */
//    public void add(String word) {
//        addElement(word);
//    }

    /**
     *  Print a word list on one line.
     */
    public void print(int indent) {
        for (int i = 0; i < size(); i++) {
            String s = get(i);
            System.out.print(s + "  ");
        }
        System.out.println();
    }

    /**
     *  Find a string in a word list.
     *  Return true if the word is in the list, false otherwise.
     */
    boolean find(String s) {
        for (int i = 0; i < size(); i++) {
            if (s.equals(get(i))) return true;
        }
        return false;
    }

}

