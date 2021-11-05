import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class NNPlayer extends PokerrPlayer{

	NeuralNetwork nn;
	int training_threshold = 1250;
	boolean trained = false;
	int inputSize = 15;
	
	LinkedList<double[]> keys = new LinkedList<>();
	LinkedList<double[]> keyOutcome = new LinkedList<>();
	int decisionIndex = 0;
	int decidedIndex = 1;
	
	LinkedList<double[]> activeKeys = new LinkedList<>();
	int startBank = 0;
	
	NNPlayer(PokerMain parent) {
		super(parent);
		inputSize += parent.players.size();
		nn = new NeuralNetwork(inputSize, 2, 1, true);
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
			
			int toReturn = BB;//getBet() == 0 ? BB : 0;
			toReturn = sanitizeBet(BB);
			addKey(toReturn);
			parent.qPrint(Arrays.toString(keys.getLast()));
			
			return toReturn;
		} else {
			if (!trained) {
				parent.qPrint(name + ": Training!");;
				nn = new NeuralNetwork(inputSize, inputSize/2, 1, true);
				nn.fit(keys.toArray(new double[][] {}),keyOutcome.toArray(new double[][] {}),2500,0);
				// logging set to 0, shows training time and average error
				trained = true;
			}
			else {
				int toReturn = 0;
				addKey(toReturn);
				keys.getLast()[decidedIndex] = 1;
				parent.qPrint(Arrays.toString(keys.getLast()));
				List<Double> evaluation = nn.predict(keys.getLast());
				parent.qPrint(name + ": The above play would probably lead to a net: " + evaluation.toString());
				if (evaluation.get(0) < 0.5) return -1; //else return 0;
				toReturn = (int) (((getGameStage(parent.board)*0.05) + Math.round(evaluation.get(0)*0.25)-0.5)*bank);//(STARTING_BANK));
				toReturn = sanitizeBetV(toReturn);
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
				double num = ((double)(bank - startBank))/((double)(parent.players.size()*STARTING_BANK));
				//parent.qPrint(Double.toString(num));
				num /= 2;
				//parent.qPrint(Double.toString(num));
				num += 0.5;
				//parent.qPrint(Double.toString(num));
				keyOutcome.add(keyOutcome.size() - index + 1, new double[]{num});// = bank - startBank;
				i[decidedIndex] = 1;
				parent.qPrint(Arrays.toString(i));
				parent.qPrint(Arrays.toString(keyOutcome.get(keyOutcome.size() - index)));
				index++;
			}
			activeKeys.clear();
			startBank = bank;
		}
	}
	
	@Override
	void winFdbk(boolean win, Card[] winningHand, int potAmt) {
		updateKeyOutcomes();
	}

	void addKey(int decision) {
		int nullvalue = 0;
		int[] bestHand = strength(bestHand(parent.board, true));
		int[] bestHandBoard = getGameStage(parent.board) != 0 ? strength(bestHand(parent.board, false)) : new int[] {0,0,0,0};
		LinkedList<Double> keyList = new LinkedList<>();
		keyList.add((double)decision/(double)(parent.players.size()*STARTING_BANK));
		keyList.add(0.0);
		/*keyList.add((double)holeCards[0].value/14);
		keyList.add((double)holeCards[0].suit/4);
		keyList.add((double)holeCards[1].value/14);
		keyList.add((double)holeCards[1].suit/4);*/
		//keyList.add((double)getBet()/(parent.players.size()*STARTING_BANK));
		keyList.add((double)getGameStage(parent.board)/4);
		keyList.add((double) bestHand[0]/10);
		keyList.add((double) bestHand[2]/14);
		keyList.add((double) bestHandBoard[0]/10);
		keyList.add((double) bestHandBoard[2]/14);
		for (PokerrPlayer p : parent.players) {
			if (p != this) keyList.add(inTheHand == true ? 1.0 : 0.0);
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
