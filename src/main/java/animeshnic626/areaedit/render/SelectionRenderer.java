package animeshnic626.areaedit.render;

import animeshnic626.areaedit.selection.ColumnPos;
import animeshnic626.areaedit.selection.ColumnSelection;
import animeshnic626.areaedit.selection.SelectionManager;
import animeshnic626.areaedit.selection.SelectionState;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Map;

public class SelectionRenderer {

    public static void render(PoseStack poseStack, Frustum frustum) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Map<ColumnPos, ColumnSelection> columns = SelectionManager.getSelectedColumns();
        if (columns.isEmpty()) return;

        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();

        poseStack.pushPose();
        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();

        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (Map.Entry<ColumnPos, ColumnSelection> entry : columns.entrySet()) {
            ColumnPos pos = entry.getKey();
            ColumnSelection sel = entry.getValue();

            int yMin = sel.getYMin();
            int yMax = sel.getYMax();

            if (SelectionState.isExtendedTo626()) {
                yMax = 626;
            } else if (SelectionState.getCustomMaxY() != null) {
                yMax = Math.min(yMax, SelectionState.getCustomMaxY());
            }

            // Проверка видимости во фрустуме по абсолютным мировым координатам
            AABB worldBox = new AABB(pos.x(), yMin, pos.z(), pos.x() + 1.0, yMax + 1.0, pos.z() + 1.0);
            if (frustum != null && !frustum.isVisible(worldBox)) {
                continue;
            }

            boolean hasNorth = isSameNeighbor(columns, pos.x(), pos.z() - 1, yMin, yMax);
            boolean hasSouth = isSameNeighbor(columns, pos.x(), pos.z() + 1, yMin, yMax);
            boolean hasWest  = isSameNeighbor(columns, pos.x() - 1, pos.z(), yMin, yMax);
            boolean hasEast  = isSameNeighbor(columns, pos.x() + 1, pos.z(), yMin, yMax);

            float inflate = 0.002f;
            float minX = (float) (pos.x() - camPos.x - (hasWest ? 0.0f : inflate));
            float maxX = (float) (pos.x() + 1.0f - camPos.x + (hasEast ? 0.0f : inflate));
            float minZ = (float) (pos.z() - camPos.z - (hasNorth ? 0.0f : inflate));
            float maxZ = (float) (pos.z() + 1.0f - camPos.z + (hasSouth ? 0.0f : inflate));
            float minY = (float) (yMin - camPos.y - inflate);
            float maxY = (float) (yMax + 1.0f - camPos.y + inflate);

            renderOuterFaces(matrix, bufferBuilder, minX, minY, minZ, maxX, maxY, maxZ,
                    !hasNorth, !hasSouth, !hasWest, !hasEast,
                    1.0f, 0.41f, 0.71f, 0.35f);
        }

        tesselator.end();

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();

        poseStack.popPose();
    }

    private static boolean isSameNeighbor(Map<ColumnPos, ColumnSelection> columns, int x, int z, int yMin, int yMax) {
        ColumnSelection neighbor = columns.get(new ColumnPos(x, z));
        if (neighbor == null) return false;
        int nMax = SelectionState.isExtendedTo626() ? 626 : (SelectionState.getCustomMaxY() != null ? Math.min(neighbor.getYMax(), SelectionState.getCustomMaxY()) : neighbor.getYMax());
        return neighbor.getYMin() == yMin && nMax == yMax;
    }

    private static void renderOuterFaces(Matrix4f mat, VertexConsumer consumer,
                                         float minX, float minY, float minZ,
                                         float maxX, float maxY, float maxZ,
                                         boolean drawNorth, boolean drawSouth,
                                         boolean drawWest, boolean drawEast,
                                         float r, float g, float bCol, float a) {
        // Down
        consumer.vertex(mat, minX, minY, minZ).color(r, g, bCol, a).endVertex();
        consumer.vertex(mat, maxX, minY, minZ).color(r, g, bCol, a).endVertex();
        consumer.vertex(mat, maxX, minY, maxZ).color(r, g, bCol, a).endVertex();
        consumer.vertex(mat, minX, minY, maxZ).color(r, g, bCol, a).endVertex();

        // Up
        consumer.vertex(mat, minX, maxY, minZ).color(r, g, bCol, a).endVertex();
        consumer.vertex(mat, minX, maxY, maxZ).color(r, g, bCol, a).endVertex();
        consumer.vertex(mat, maxX, maxY, maxZ).color(r, g, bCol, a).endVertex();
        consumer.vertex(mat, maxX, maxY, minZ).color(r, g, bCol, a).endVertex();

        // North
        if (drawNorth) {
            consumer.vertex(mat, minX, minY, minZ).color(r, g, bCol, a).endVertex();
            consumer.vertex(mat, minX, maxY, minZ).color(r, g, bCol, a).endVertex();
            consumer.vertex(mat, maxX, maxY, minZ).color(r, g, bCol, a).endVertex();
            consumer.vertex(mat, maxX, minY, minZ).color(r, g, bCol, a).endVertex();
        }

        // South
        if (drawSouth) {
            consumer.vertex(mat, minX, minY, maxZ).color(r, g, bCol, a).endVertex();
            consumer.vertex(mat, maxX, minY, maxZ).color(r, g, bCol, a).endVertex();
            consumer.vertex(mat, maxX, maxY, maxZ).color(r, g, bCol, a).endVertex();
            consumer.vertex(mat, minX, maxY, maxZ).color(r, g, bCol, a).endVertex();
        }

        // West
        if (drawWest) {
            consumer.vertex(mat, minX, minY, minZ).color(r, g, bCol, a).endVertex();
            consumer.vertex(mat, minX, minY, maxZ).color(r, g, bCol, a).endVertex();
            consumer.vertex(mat, minX, maxY, maxZ).color(r, g, bCol, a).endVertex();
            consumer.vertex(mat, minX, maxY, minZ).color(r, g, bCol, a).endVertex();
        }

        // East
        if (drawEast) {
            consumer.vertex(mat, maxX, minY, minZ).color(r, g, bCol, a).endVertex();
            consumer.vertex(mat, maxX, maxY, minZ).color(r, g, bCol, a).endVertex();
            consumer.vertex(mat, maxX, maxY, maxZ).color(r, g, bCol, a).endVertex();
            consumer.vertex(mat, maxX, minY, maxZ).color(r, g, bCol, a).endVertex();
        }
    }
}
