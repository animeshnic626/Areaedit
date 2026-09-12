package animeshnic626.areaedit.client;

import animeshnic626.areaedit.Areaedit;
import animeshnic626.areaedit.math.ColumnPos;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = Areaedit.MODID, value = Dist.CLIENT)
public class AreaRenderer {

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        VertexConsumer buffer = mc.renderBuffers().bufferSource().getBuffer(RenderType.lines());

        Frustum frustum = new Frustum(new Matrix4f(poseStack.last().pose()), new Matrix4f(event.getProjectionMatrix()));
        frustum.prepare(camPos.x, camPos.y, camPos.z);

        synchronized (Areaedit.selectedColumns) {
            for (ColumnPos col : Areaedit.selectedColumns) {
                int topY = Areaedit.height626Mode ? 626 : col.maxY;
                int minY = Math.min(col.minY, topY);
                int maxY = Math.max(col.minY, topY);

                AABB box = new AABB(col.x, minY, col.z, col.x + 1.0, maxY + 1.0, col.z + 1.0);
                if (frustum.isVisible(box)) {
                    LevelRenderer.renderLineBox(poseStack, buffer, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, 1.0f, 0.4f, 0.7f, 0.8f);
                }
            }
        }

        synchronized (Areaedit.filledBlocks) {
            for (BlockPos pos : Areaedit.filledBlocks) {
                AABB box = new AABB(pos);
                if (frustum.isVisible(box)) {
                    LevelRenderer.renderLineBox(poseStack, buffer, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, 1.0f, 0.2f, 0.5f, 0.6f);
                }
            }
        }

        mc.renderBuffers().bufferSource().endBatch(RenderType.lines());
        poseStack.popPose();
    }
}