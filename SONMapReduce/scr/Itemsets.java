import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

public class Itemsets{

	private final List<List<Itemset>> itemsetsLevels = new ArrayList<>();

	public Itemsets() {
		itemsetsLevels.add(new ArrayList<>());
	}


	public void addItemset(Itemset itemset, int k) {
		while (itemsetsLevels.size() <= k) {
			itemsetsLevels.add(new ArrayList<>());
		}
		itemsetsLevels.get(k).add(itemset);
	}

	public List getItemsets() {
		List<String> freqItems = new ArrayList<>();
		for (List<Itemset> level : itemsetsLevels) {
			for (Itemset itemset : level) {
				Arrays.sort(itemset.itemset);
				freqItems.add(itemset.toString());
			}
		}
		return freqItems;
	}


}
