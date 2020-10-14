import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Map.Entry;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;

public class Apriori {

	protected int k;
	private int sizeOfdb;
	protected long endTimestamp;
	protected long startTimestamp;
	private int minSupportRelative;
	protected int countOfCandidate = 0;
	protected Itemsets template = null;
	private List<int[]> database = null;

	private List<Itemset> candidateGeneration(List<Integer> frequent1) {
		List<Itemset> candidates = new ArrayList<>();

		for (int i = 0; i < frequent1.size(); i++) {
			Integer item1 = frequent1.get(i);
			for (int j = i + 1; j < frequent1.size(); j++) {
				Integer item2 = frequent1.get(j);
				candidates.add(new Itemset(new int []{item1, item2}));
			}
		}
		return candidates;
	}

	protected List<Itemset> candidateGenerationSizeK(List<Itemset> levelK_1) {

		List<Itemset> candidates = new ArrayList<>();

		loop1: for (int i = 0; i < levelK_1.size(); i++) {
			int[] itemset1 = levelK_1.get(i).itemset;
			loop2: for (int j = i + 1; j < levelK_1.size(); j++) {
				int[] itemset2 = levelK_1.get(j).itemset;
				for (int k = 0; k < itemset1.length; k++) {
					if (k == itemset1.length - 1) {
						if (itemset1[k] >= itemset2[k]) {
							continue loop1;
						}
					}
					else if (itemset1[k] < itemset2[k]) {
						continue loop2;
					} else if (itemset1[k] > itemset2[k]) {
						continue loop1;
					}
				}

				int newItemset[] = new int[itemset1.length+1];
				System.arraycopy(itemset1, 0, newItemset, 0, itemset1.length);
				newItemset[itemset1.length] = itemset2[itemset2.length -1];

				if (sizeK_1OfAllSubsets_Frequent(newItemset, levelK_1)) {
					candidates.add(new Itemset(newItemset));
				}
			}
		}
		return candidates;
	}

	void itemsetSaving(Itemset itemset) {
		template.addItemset(itemset, itemset.itemset.length);
	}

	void itemsetSavingToFile(Integer item, Integer support) {
		Itemset itemset = new Itemset(item);
		itemset.support = support;
		template.addItemset(itemset, 1);
	}

	public Itemsets algoExecution(double minimumSupport, String input, String output) throws IOException {

		if(output == null){
			template =  new Itemsets();
		}else{
			template = null;
		}

		startTimestamp = System.currentTimeMillis();
		countOfCandidate = 0;
		sizeOfdb = 0;
		Map<Integer, Integer> itemCountMapping = new HashMap<Integer, Integer>();

		database = new ArrayList<>();

		String [] data_in = input.split("\n");
		String line;

		for (int j=0;j<data_in.length;j++) {
			line=data_in[j].trim();
			String[] splitedLine = line.split(" ");

			int transaction[] = new int[splitedLine.length];

			for (int i=0; i< splitedLine.length; i++) {
				String line_split = splitedLine[i].replaceAll("[\\.]","");
				Integer item = Integer.parseInt(line_split);
				transaction[i] = item;
				Integer count = itemCountMapping.get(item);
				if (count == null) {
					itemCountMapping.put(item, 1);
				} else {
					itemCountMapping.put(item, ++count);
				}
			}
			database.add(transaction);
			sizeOfdb++;
		}

		this.minSupportRelative = (int) Math.ceil(minimumSupport * sizeOfdb);
		k = 1;

		List<Integer> frequent1 = new ArrayList<>();
		for(Entry<Integer, Integer> entry : itemCountMapping.entrySet()){
			if(entry.getValue() >= minSupportRelative){
				frequent1.add(entry.getKey());
				itemsetSavingToFile(entry.getKey(), entry.getValue());
			}
		}

		Collections.sort(frequent1, new Comparator<Integer>() {
			public int compare(Integer o1, Integer o2) {
				return o1 - o2;
			}
		});

		if(frequent1.size() == 0){
			return template;
		}

		countOfCandidate += frequent1.size();

		List<Itemset> level = null;
		k = 2;
		do{
			List<Itemset> candidatesK;

			if(k ==2){
				candidatesK = candidateGeneration(frequent1);
			}else{
				candidatesK = candidateGenerationSizeK(level);
			}

			countOfCandidate += candidatesK.size();

			for(int[] transaction: database){
				if(transaction.length < k) {
					continue;
				}
				loopCand:	for(Itemset candidate : candidatesK){
					int pos = 0;
					for(int item: transaction){
						if(item == candidate.itemset[pos]){
							pos++;
							if(pos == candidate.itemset.length){
								candidate.support++;
								continue loopCand;
							}
						}else if(item > candidate.itemset[pos]){
							continue loopCand;
						}
					}
				}
			}

			level = new ArrayList<Itemset>();
			for (Itemset candidate : candidatesK) {
				if (candidate.support >= minSupportRelative) {
					level.add(candidate);
					itemsetSaving(candidate);
				}
			}
			k++;
		}while(level.isEmpty() == false);

		endTimestamp = System.currentTimeMillis();

		return template;
	}

	public static int isSame(int [] itemset1, int [] itemsets2, int posRmv) {
		int j=0;
		for(int i=0; i<itemset1.length; i++){
			if(j == posRmv){
				j++;
			}
			if(itemset1[i] == itemsets2[j]){
				j++;
			}else if (itemset1[i] > itemsets2[j]){
				return 1;
			}else{
				return -1;
			}
		}
		return 0;
	}

	protected boolean sizeK_1OfAllSubsets_Frequent(int[] candidate, List<Itemset> levelK_1) {
		for(int posRmv=0; posRmv< candidate.length; posRmv++){
			int initial = 0;
			int last = levelK_1.size() - 1;
			boolean found = false;
			while( initial <= last )
			{
				int mid = ( initial + last ) >>1 ;
				int contrast = isSame(levelK_1.get(mid).itemset, candidate, posRmv);
				if(contrast < 0 ){
					initial = mid + 1;
				}
				else if(contrast  > 0 ){
					last = mid - 1;
				}
				else{
					found = true;
					break;
				}
			}

			if(found == false){
				return false;
			}
		}
		return true;
	}
}
