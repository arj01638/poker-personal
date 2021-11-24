import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class NNPlayer extends PokerPlayer {

	NeuralNetwork nn;
	int training_threshold = 950;
	boolean trained = false;
	int inputSize = 15;
	double trainingWinnings = 0;

	LinkedList<double[]> keys = new LinkedList<>();
	LinkedList<double[]> keyOutcome = new LinkedList<>();
	int decidedIndex = 0;

	LinkedList<double[]> activeKeys = new LinkedList<>();
	int startBank = 0;
	
	NNPlayer(PokerMain parent) {
		super(parent);
		inputSize += parent.players.size();
		//nn = new NeuralNetwork(inputSize, 3, 1, true);
	}

	@Override
	int evaluate() {
		if (parent.committedPlayers().size() == 0 
				|| (parent.activePlayers().size() == 1 && parent.activePlayers().get(0) == this)
				|| (parent.committedPlayers().size() == 1 && parent.committedPlayers().get(0) == this)) {
			parent.qPrint(name + ": No players...");
			return 0;
		}
		if (getGameStage(parent.board) == 0) {
			updateKeyOutcomes();
		}
		
		if (parent.iterations == 25 && parent.iterations2 == 1) {
			for (int i = 0; i < keys.size(); i++) {
				parent.qPrint(Arrays.toString(keys.get(i)));
				parent.qPrint(Arrays.toString(keyOutcome.get(i)));
			}
		}
		
		
		if (parent.iterations < training_threshold) {
			
			int toReturn = BB;
			toReturn = sanitizeBet(BB);
			addKey(toReturn);
			parent.qPrint(Arrays.toString(keys.getLast()));
			
			return toReturn;
		} else {
			if (!trained) {
				parent.qPrint(name + ": Training!");;
				nn = new NeuralNetwork(inputSize, 3, 1, true);
				consolidateKeys();
				normalizeOutcomes();
				//printKeys();
				nn.fit(keys.toArray(new double[][] {}),keyOutcome.toArray(new double[][] {}),2000,0);
				// logging set to 0, shows training time and average error
				trained = true;
				trainingWinnings = totalWinnings;
			}
			else {
				parent.qPrint(name + "current performance: "
						+ (totalWinnings-trainingWinnings)
						+ "(" + (totalWinnings-trainingWinnings)/(parent.ROUNDS-training_threshold) + ")");
				int toReturn = 0;
				addKey(toReturn);
				keys.getLast()[decidedIndex] = 1;
				parent.qPrint(Arrays.toString(keys.getLast()));
				List<Double> evaluation = nn.predict(keys.getLast());
				parent.qPrint(name + ": The above play would probably lead to a net: " + evaluation.toString());
				if (evaluation.get(0) < 0.5) return -1; //else return 0;
				toReturn = (int) (Math.round((evaluation.get(0)-0.5)*0.5*bank));
				toReturn = sanitizeBet(toReturn);
				return toReturn;
			}
		}
		return -1;
	}
	
	void updateKeyOutcomes() {
		if (!keys.isEmpty() && keys.getLast()[decidedIndex] == 0) {
			parent.qPrint(name + ": Last keys:");
			int index = 1;
			for (double[]  i : activeKeys) {
				keyOutcome.remove(keyOutcome.size() - index);
				double num = ((double)(bank - startBank));///((double)(parent.players.size()*STARTING_BANK));
				//num /= 2;
				//num += 0.5;
				keyOutcome.add(keyOutcome.size() - index + 1, new double[]{num});// = bank - startBank;
				i[decidedIndex] = 1;
				parent.qPrint(Arrays.toString(i));
				parent.qPrint(Arrays.toString(keyOutcome.get(keyOutcome.size() - index)));
				index++;
			}
			startBank = bank;

			activeKeys.clear();
		}
	}
	
	@Override
	void winFdbk(boolean win, Card[] winningHand, int potAmt) {
		updateKeyOutcomes();
	}

	void printKeys() {
		for (double[] i : keys) {
			System.out.println(Arrays.toString(i));
			System.out.println(Arrays.toString(keyOutcome.get(keys.indexOf(i))));
		}
	}

	void consolidateKeys() {
		double startSize = keys.size();
		int total = 0;
		int sub = 0;
		int index = 0;
		boolean loop = true;
		ArrayList<double[]> keys2 = new ArrayList<>(keys);
		while (loop) {
			double[] key = keys2.get(index);
			for (int i = (int)(Math.random()*0.5*keys2.size()); i < keys2.size(); i++) {
				double[] key2 = keys2.get(i);
				if (key != key2 && Arrays.compare(key, key2) == 0) {
					keys.remove(i);
					keys2.remove(i);
					keyOutcome.get(index)[0] += keyOutcome.get(i)[0];
					keyOutcome.remove(i);
					sub++;
					total++;
					break;
				}
			}
			index++;
			if (index % 100 == 0) {
				if (sub == 0) loop = false;
				sub = 0;
			}
			if (index >= keys.size()) index = 0;
		}
		parent.qPrint(name + ": " + total + " consolidations");
		parent.qPrint(name + ": " + (double)keys.size()/startSize + " ratio");
	}

	void normalizeOutcomes() {
		double max = 0;
		for (double[] i : keyOutcome) {
			if (Math.abs(i[0]) > max) max = Math.abs(i[0]);
		}
		for (int i = 0; i < keyOutcome.size(); i++) {
			double[] num = new double[] {(((double)keyOutcome.get(i)[0]/max)/2.0)+0.5};
			keyOutcome.remove(i);
			keyOutcome.add(i,num);
		}
	}

	void addKey(int decision) {
		int[] bestHand = bh != null ? strength(bh) : strength(bestHand(parent.board,true));
		int[] bestHandBoard = getGameStage(parent.board) != 0 ? strength(bestHand(parent.board, false)) : new int[] {0,0,0,0};
		LinkedList<Double> keyList = new LinkedList<>();
		//keyList.add((double)decision/(double)(parent.players.size()*STARTING_BANK));
		keyList.add(0.0);
		//keyList.add((double)holeCards[0].value/14);
		//keyList.add((double)holeCards[0].suit/4);
		//keyList.add((double)holeCards[1].value/14);
		//keyList.add((double)holeCards[1].suit/4);
		//keyList.add((double)getBet()/(parent.players.size()*STARTING_BANK));
		keyList.add((double)getGameStage(parent.board)/4.0);
		keyList.add((double) bestHand[0]/9.0);
		keyList.add((double) bestHand[2]/14.0);
		keyList.add((double) bestHandBoard[0]/9.0);
		keyList.add((double) bestHandBoard[2]/14.0);
		//keyList.add(((double)bestHand[0] - (double)bestHandBoard[0])/9.0);
		for (PokerPlayer p : parent.players) {
			if (p != this) keyList.add(p.inTheHand ? 1.0 : 0.0);
		}
		inputSize = keyList.size();
		double[] key = new double[inputSize];//keyList.toArray(new double[]{});
		for (int i = 0; i < keyList.size(); i++) {
			key[i] = keyList.get(i);
		}
		keys.add(key);
		activeKeys.add(key);
		keyOutcome.add(new double[]{0});
	}



	private int factorial(int number) {
		int fact = 1;
		for(int i=1;i<=number;i++){
			fact=fact*i;
		}
		return fact;
	}
}
