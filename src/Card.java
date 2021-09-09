
public class Card implements Comparable<Card> {

	int value;
	int suit;
	
	Card(int value, int suit) {
		this.value = value;
		this.suit = suit;
	} // Card
	
	@Override
	public String toString() {
		return Integer.toString(value) + "." + Integer.toString(suit);
	}

	@Override
	public int compareTo(Card o) {
		return o.value - this.value;
	}
	
	public int compareToS(Card o) {
		return o.suit - this.suit;
	}
	
}
