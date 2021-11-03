import java.util.LinkedList;

public class Pot {

	LinkedList<PokerrPlayer> players;
	int potAmt;
	public int bet;
	int[] callersAmt;
	
	
	Pot(LinkedList<PokerrPlayer> players, int length) {
		callersAmt = new int[length];
		this.players = players;
		this.potAmt = 0;
	}
	
	
}
