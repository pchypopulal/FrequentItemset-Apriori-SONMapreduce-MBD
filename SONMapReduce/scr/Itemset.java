public class Itemset{

    public int support = 0;
    public int[] itemset;

    public Itemset(int item) {
        itemset = new int[]{item};
    }

    public Itemset(int[] items) {
        this.itemset = items;
    }

    public Integer get(int position) {
        return itemset[position];
    }

    public String toString(){

        StringBuffer r = new StringBuffer ();

        for(int i=0; i< itemset.length; i++){
            r.append(get(i));
            r.append(' ');
        }
        return r.toString();
    }
}
