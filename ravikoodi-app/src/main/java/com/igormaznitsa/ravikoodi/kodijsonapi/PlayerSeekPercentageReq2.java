package com.igormaznitsa.ravikoodi.kodijsonapi;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PlayerSeekPercentageReq2 extends PlayerIdReq {

    public static class Percentage {

        @JsonProperty("percentage")
        private final double percentage;

        public Percentage(final double percentage) {
            this.percentage = percentage;
        }
    }

    @JsonProperty("value")
    private final Percentage percentage;

    public PlayerSeekPercentageReq2(final ActivePlayerInfo player, final double percentage) {
        super(player);
        this.percentage = new Percentage(percentage);
    }

    public Percentage getPercentage() {
        return this.percentage;
    }
}
