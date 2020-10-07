package hohserg.elegant.networking.test;

public interface HotSlot {

    int index();

    class _1 implements HotSlot {
        @Override
        public int index() {
            return 0;
        }
    }

    class _2 implements HotSlot {
        @Override
        public int index() {
            return 1;
        }
    }
}
