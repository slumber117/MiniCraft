package minicraft.item;

public class ItemStack {
    private Item item;
    private int count;

    public ItemStack(Item item, int count) {
        this.item = item;
        this.count = count;
    }

    public Item getItem() { return item; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
    public void add(int amount) { this.count += amount; }
    public void remove(int amount) { this.count -= amount; }
    public boolean isEmpty() { return item == null || count <= 0; }

    public String getDisplayName() {
        return item != null ? item.getDisplayName() : "";
    }
}
