package com.replaymod.online.api.replay.holders;

import lombok.Data;

@Data
public class Rating {
    private int negative, positive;

    public enum RatingType {

        LIKE("like"), DISLIKE("dislike"), NEUTRAL("neutral");

        private String key;

        public String getKey() { return key; }

        RatingType(String key) {
            this.key = key;
        }

        public static RatingType fromBoolean(Boolean rating) {
            return rating == null ? RatingType.NEUTRAL :
                    (rating ? RatingType.LIKE : RatingType.DISLIKE);
        }
    }
}
