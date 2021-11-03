import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class NNPlayer extends PokerrPlayer{

	NeuralNetwork nn;
	int training_threshold = 500;
	boolean trained = false;
	
	
	LinkedList<double[]> keys = new LinkedList<>();
	LinkedList<double[]> keyOutcome = new LinkedList<>();;
	int decisionIndex = 17;
	int decidedIndex = 18;
	
	LinkedList<double[]> activeKeys = new LinkedList<>();
	int startBank = 0;
	
	NNPlayer(PokerrMain parent) {
		super(parent);
		nn = new NeuralNetwork(19, 14, 1, true); 
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
			
			int toReturn = (int) (Math.random()*0.5*STARTING_BANK);
			toReturn = sanitizeBet(toReturn);
			addKey(toReturn);
			//keys.getLast()[decisionIndex] = (double) toReturn;
			parent.qPrint(Arrays.toString(keys.getLast()));
			
			return toReturn;
		} else {
			if (!trained) {
				parent.qPrint(name + ": Training!");
				nn.fit(keys.toArray(new double[][] {}),keyOutcome.toArray(new double[][] {}),1000,1); 
				// logging set to 0, shows training time and average error
				trained = true;
			}
			else {
				int toReturn = (int) STARTING_BANK;//(Math.random()*STARTING_BANK);
				toReturn = sanitizeBet(toReturn);
				addKey(toReturn);
				keys.getLast()[decidedIndex] = 1;
				parent.qPrint(Arrays.toString(keys.getLast()));
				List<Double> evaluation = nn.predict(keys.getLast());
				parent.qPrint(name + ": The above play would probably lead to a net: " + evaluation.toString());
				toReturn = (int) (Math.round(evaluation.get(0))*(STARTING_BANK));
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
				keyOutcome.add(keyOutcome.size() - index + 1, new double[]{(((bank - startBank)/(parent.players.size()*STARTING_BANK))/2)+1});// = bank - startBank;
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
		double[] key = {
			(double)holeCards[0].value/14,
			(double)holeCards[0].suit/4,
			(double)holeCards[1].value/14,
			(double)holeCards[1].suit/4,
			parent.board[0] != null ? (double)parent.board[0].value/14 : nullvalue,
			parent.board[0] != null ? (double)parent.board[0].suit/4 : nullvalue,
			parent.board[1] != null ? (double)parent.board[1].value/14 : nullvalue,
			parent.board[1] != null ? (double)parent.board[1].suit/4 : nullvalue,
			parent.board[2] != null ? (double)parent.board[2].value/14 : nullvalue,
			parent.board[2] != null ? (double)parent.board[2].suit/4 : nullvalue,
			parent.board[3] != null ? (double)parent.board[3].value/14 : nullvalue,
			parent.board[3] != null ? (double)parent.board[3].suit/4 : nullvalue,
			parent.board[4] != null ? (double)parent.board[4].value/14 : nullvalue,
			parent.board[4] != null ? (double)parent.board[4].suit/4 : nullvalue,
			(double)getBet()/(parent.players.size()*STARTING_BANK),
			(double)getPlayerHash(parent.committedPlayers())/(Math.pow(10,parent.players.size())),
			(double)getGameStage(parent.board)/4,
			decision/(parent.players.size()*STARTING_BANK), //decision
			0 //decided
		};
		keys.add(key);
		activeKeys.add(key);
		keyOutcome.add(new double[]{0});
	}


	int getPlayerHash(LinkedList<PokerrPlayer> x) {
		int hash = 0;
		for (PokerrPlayer p : x) {
			hash += Math.pow(10, x.indexOf(p));
		}
		return hash;
	}
	
	int sanitizeBet(int tr) {
		int toReturn = tr;
		if (getBet() > toReturn) return -1;
		if (getBet() == toReturn) return 0;
		toReturn = getBet() - toReturn;
		if (toReturn + getBet() < BB)
			toReturn = BB - getBet();
		if (toReturn + getBet() < getBet() * 2)
			toReturn = 0;
		if (toReturn + getBet() > bank)
			toReturn = bank - (toReturn + getBet());
		if (toReturn < 0)
			toReturn = 0;
		return toReturn;
	}
	
}
