package scheme.ai;

import arc.func.Cons;
import arc.math.geom.Position;
import arc.math.geom.Vec2;
import arc.scene.style.Drawable;
import mindustry.entities.units.AIController;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.gen.Player;
import mindustry.world.Tile;
import mindustry.world.blocks.ConstructBlock.ConstructBuild;
import scheme.moded.ModedBinding;

import static arc.Core.*;
import static mindustry.Vars.*;
import static scheme.SchemeVars.*;

public class GammaAI extends AIController {

    public static final String tooltip = bundle.format("gamma.tooltip", keybind());

    public static Updater move = Updater.none;
    public static Updater build = Updater.none;
    public static float range = 80f;

    public Player target;
    public Position cache;
    public Position aim;

    @Override
    public void updateUnit() {
        if (target == null || !target.isAdded()) return;

        move.update.get(this);
        build.update.get(this);

        player.boosting = target.boosting;
        aim = new Vec2(target.mouseX, target.mouseY);
    }

    public void draw() {
        if (target != null && target != player) render.drawPlans(target.unit(), build != Updater.destroy);
    }

    public void block(Tile tile, boolean breaking) {
        Building build = builds.get(tile);
        unit.addBuild(breaking
                ? new BuildPlan(tile.x, tile.y, build.rotation, build instanceof ConstructBuild c ? c.previous : build.block)
                : new BuildPlan(tile.x, tile.y));
    }

    public void cache() {
        target = ai.players.get();
        cache = target == player ? player.tileOn() : target;
    }

    /** Key to press to disable ai. */
    public static String keybind() {
        return keybinds.get(ModedBinding.alternative).key.toString() + " + " + keybinds.get(ModedBinding.toggle_ai).key.toString();
    }

    public enum Updater {
        none(Icon.line, ai -> {}),
        circle(Icon.commandRally, ai -> {
            ai.circle(ai.cache, range);
            ai.faceMovement();
            ai.stopShooting();
        }),
        cursor(Icon.diagonal, ai -> moveTo(ai, ai.aim)),
        follow(Icon.resize, ai -> moveTo(ai, ai.cache)),
        help(Icon.add, ai -> {
            if (ai.target.unit().plans.isEmpty() || !ai.target.unit().updateBuilding) return;
            ai.unit.addBuild(ai.target.unit().buildPlan());
        }),
        destroy(Icon.hammer, ai -> {}); // works through events

        public final Drawable icon;
        public final Cons<GammaAI> update;

        private Updater(Drawable icon, Cons<GammaAI> update) {
            this.icon = icon;
            this.update = update;
        }

        public String tooltip() {
            return "@gamma." + name();
        }

        private static void moveTo(GammaAI ai, Position pos) {
            ai.moveTo(pos, range / 3f);
            if (ai.unit.vel.len() > .5f) ai.faceMovement();
            else ai.unit.lookAt(ai.aim);
            ai.unit.aim(ai.aim);
            ai.unit.controlWeapons(true, ai.target.shooting);
        }
    }
}