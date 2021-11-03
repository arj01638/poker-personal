
public class Card implements Comparable<Card> {

	int value;
	int suit;
	
	Card(int value, int suit) {
		this.value = value;
		this.suit = suit;
	} // Card
	
	@Override
	public String toString() {
		return value + "." + suit;
	}

	@Override
	public int compareTo(Card o) {
		return o.value - this.value;
	}
	
	public int compareToS(Card o) {
		if (o.suit != this.suit) return 1;
		return 0;
	}
	
}
