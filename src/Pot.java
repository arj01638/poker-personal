import java.util.LinkedList;

public class Pot {

	LinkedList<PokerrPlayer> players;
	int potAmt;
	public int bet;
	int[] callersAmt;
	
	
	Pot(LinkedList<PokerrPlayer> players, int potAmt) {
		callersAmt = new int[30];
		this.players = players;
		this.potAmt = potAmt;
	}
	
	
}
