package eliza;

import java.util.ArrayList;


/**
 *  Eliza reassembly list.
 */
@SuppressWarnings("serial")
public class ReasembList extends ArrayList<String> {



    /**
     *  Print the reassembly list.
     */
    public void print(int indent) {
        for (int i = 0; i < size(); i++) {
            for (int j = 0; j < indent; j++) System.out.print(" ");
            String s = get(i);
            System.out.println("reasemb: " + s);
        }
    }
}

