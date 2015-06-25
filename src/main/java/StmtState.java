
enum StmtState {
    START {
        StmtState next(StmtState state, char c) {
            switch (c) {
                case '\'': state = APOS; break;
                case '"': state = QUOT; break;
                case ';': state = END; break;
                default:
            }
            return state;
        }
    },
    APOS {
        StmtState next(StmtState state, char c) {
            switch (c) {
                case '\'': state = APOS2; break;
                default:
            }
            return state;
        }
    },
    APOS2 {
        StmtState next(StmtState state, char c) {
            switch (c) {
                case '\'': state = APOS; break;
                case ';': state = END; break;
                default: state = START;
            }
            return state;
        }
    },
    QUOT {
        StmtState next(StmtState state, char c) {
            switch (c) {
                case '"': state = QUOT2; break;
                default:
            }
            return state;
        }
    },
    QUOT2 {
        StmtState next(StmtState state, char c) {
            switch (c) {
                case '"': state = APOS; break;
                case ';': state = END; break;
                default: state = START;
            }
            return state;
        }
    },
    END {
        StmtState next(StmtState state, char c) {
            return state;
        }
    };

    /**
     * Constructor.
     */
    private StmtState() {}

    StmtState next(StmtState state, char c) {
        throw new RuntimeException("what");
    }
}

