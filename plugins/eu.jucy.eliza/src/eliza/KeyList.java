package eliza;

import java.util.ArrayList;


/**
 *  Eliza key list.
 *  This stores all the keys.
 */
@SuppressWarnings("serial")
public class KeyList extends ArrayList<Key> {

    /**
     *  Add a new key.
     */
    public void add(String key, int rank, DecompList decomp) {
        add(new Key(key, rank, decomp));
    }

    /**
     *  Print all the keys.
     */
    public void print(int indent) {
        for (int i = 0; i < size(); i++) {
            Key k = (Key)get(i);
            k.print(indent);
        }
    }

    /**
     *  Search the key list for a given key.
     *  Return the Key if found, else null.
     */
    Key getKey(String s) {
        for (int i = 0; i < size(); i++) {
            Key key = get(i);
            if (s.equals(key.key())) return key;
        }
        return null;
    }

    /**
     *  Break the string s into words.
     *  For each word, if isKey is true, then push the key
     *  into the stack.
     */
    public void buildKeyStack(KeyStack stack, String s) {
        stack.reset();
        s = EString.trim(s);
        String lines[] = new String[2];
        Key k;
        while (EString.match(s, "* *", lines)) {
            k = getKey(lines[0]);
            if (k != null) stack.pushKey(k);
            s = lines[1];
        }
        k = getKey(s);
        if (k != null) stack.pushKey(k);
        //stack.print();
    }
}
