package com.patternbox.core

/** how the weapon pattern gets used */
enum class WeaponType(val label: String, val blurb: String, val dmg: Float, val swing: Float, val kb: Float, val ranged: Boolean) {
    BLADE("blade", "normal swing, does what it says", 1f, 1f, 1f, false),
    HAMMER("hammer", "slow, hits hard, sends people flying", 1.45f, 1.6f, 2.1f, false),
    SPEAR("spear", "fast poke with extra reach", .9f, .8f, .8f, false),
    BOW("bow", "shoots your weapon pattern as an arrow", .65f, 1.3f, .6f, true),
    BOOMERANG("boomerang", "throw it, it comes back. hits on the way out and in", .75f, 1.2f, .9f, true),
    BOMB("bomb", "lobs a pattern bomb. it hurts you too if you're close", 1.3f, 2f, 1.5f, true),
}

enum class WeaponMat(val label: String, val blurb: String, val dmg: Float, val kb: Float) {
    STEEL("steel", "no tricks", 1f, 1f),
    FIRE("fire", "sets them on fire for a few seconds", .85f, 1f),
    ICE("ice", "slows them down", .85f, 1f),
    SHOCK("shock", "stuns for a moment", .8f, 1f),
    BOUNCY("bouncy", "barely hurts, launches them", .55f, 2.3f),
    GLASS("glass", "huge damage, sometimes shatters", 1.8f, 1f),
}

enum class ArmourMat(val label: String, val blurb: String, val def: Float, val speed: Float, val jump: Float) {
    STEEL("steel", "solid", 1f, 0f, 0f),
    GOLD("gold", "blocks more, super heavy", 1.35f, -.45f, -1f),
    PAPER("paper", "barely armour, but you're fast", .45f, .7f, 1.2f),
    RUBBER("rubber", "bounces attackers away", .75f, 0f, .5f),
    SPIKY("spiky", "melee attackers get hurt too", .85f, -.1f, 0f),
    ICE("ice", "freezes whoever hits you", .9f, 0f, 0f),
}

enum class Ability(val label: String, val blurb: String, val cooldown: Float) {
    NONE("none", "just vibes", 0f),
    DASH("dash", "zoom forward, hurts whoever you hit", 4f),
    DOUBLE_JUMP("double jump", "jump again in the air, always on", 0f),
    SLAM("slam", "jump up and smash the ground", 7f),
    HEAL("heal", "get back 30% hp", 12f),
    TELEPORT("teleport", "pop up behind the enemy", 6f),
    RAGE("rage", "4 seconds of more damage and speed", 14f),
    BUBBLE("bubble", "can't be hurt for a moment", 9f),
}
