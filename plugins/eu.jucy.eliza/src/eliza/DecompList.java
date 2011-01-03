package eliza;

import java.util.ArrayList;


/**
 *  Eliza decomp list.
 *  This stores all the decompositions of a single key.
 */
@SuppressWarnings("serial")
public class DecompList extends ArrayList<Decomp> {

 

	/**
     *  Add another decomp rule to the list.
     */
    public void add(String word, boolean mem, ReasembList reasmb) {
        add(new Decomp(word, mem, reasmb));
    }

    /**
     *  Print the whole decomp list.
     */
    public void print(int indent) {
        for (int i = 0; i < size(); i++) {
            Decomp d = get(i);
            d.print(indent);
        }
    }
}

