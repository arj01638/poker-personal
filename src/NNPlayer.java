import java.util.HashSet;
import java.util.LinkedList;

public class NNPlayer extends PokerrPlayer{

	NeuralNetwork nn;
	int training_threshold = 1000;
	LinkedList<double[]> keys;
	LinkedList<Double> keyOutcome;
	int decisionIndex = 17;
	
	LinkedList<Key> activeKeys = new LinkedList<>();
	int startBank = 0;
	
	NNPlayer(PokerrMain parent) {
		super(parent);
		nn = new NeuralNetwork(2, 10, 1, true); 
	}

	@Override
	int evaluate() {
		if (getPlayerHash(parent.committedPlayers()) == 0) {
			parent.qPrint(name + ": No players...");
			return 0;
		}
		
		
		
		if (parent.iterations < training_threshold) {
			int toReturn = (int) (Math.random()*STARTING_BANK);
			toReturn = sanitizeBet(toReturn);
			addKey();
			keys.getLast()[decisionIndex] = toReturn;
			return toReturn;
		} else {
			
		}
		return 0;
	}

	void addKey() {
		double[] key = {
			holeCards[0].value,
			holeCards[0].suit,
			holeCards[1].value,
			holeCards[1].suit,
			parent.board[0].value,
			parent.board[0].suit,
			parent.board[1].value,
			parent.board[1].suit,
			parent.board[2].value,
			parent.board[2].suit,
			parent.board[3].value,
			parent.board[3].suit,
			parent.board[4].value,
			parent.board[4].suit,
			getBet(),
			getPlayerHash(parent.committedPlayers()),
			getGameStage(parent.board),
			1, //decision
		};
		keys.add(key);
		keyOutcome.add(0.0);
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
