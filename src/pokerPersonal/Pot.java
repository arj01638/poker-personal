package pokerPersonal;

import java.util.LinkedList;

public class Pot {

	LinkedList<PokerPlayer> players;
	int potAmt;
	public int bet;
	int[] callersAmt;
	
	
	Pot(LinkedList<PokerPlayer> players, int length) {
		callersAmt = new int[length];
		this.players = players;
		this.potAmt = 0;
	}
	
	
}
