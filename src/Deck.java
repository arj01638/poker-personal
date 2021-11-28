import java.util.LinkedList;
import java.util.concurrent.ThreadLocalRandom;

public class Deck {

	private final LinkedList<Card> mainDeck = new LinkedList<>();
	Deck() {
		this.clear();
	}
	
	public Card randomCard() {
		return mainDeck.remove(ThreadLocalRandom.current().nextInt(0, mainDeck.size()));
	}
	
	public void clear() {
		mainDeck.clear();
		for (int i = 2; i < 15; i++) {
			for (int k = 1; k < 5; k++) {
				mainDeck.add(new Card(i,k));
				//System.out.println(i+ ", " + k);
			}
		}
	} // clear
}
