package gregtech.client.model.modelfactories;

import gregtech.api.unification.material.Material;
import gregtech.api.unification.material.info.MaterialIconSet;
import gregtech.api.unification.material.info.MaterialIconType;
import gregtech.client.model.ModelFactory;
import gregtech.common.blocks.BlockFrame;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.util.vector.Vector3f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@SideOnly(Side.CLIENT)
public class FrameBakedModel implements IBakedModel {

    private final Map<MaterialIconSet, EnumMap<EnumFacing, BakedQuad>> materialFaces;
    private final ThreadLocal<TextureAtlasSprite> particle;

    public FrameBakedModel() {
        this.materialFaces = new Object2ObjectOpenHashMap<>();
        this.particle = ThreadLocal.withInitial(() -> Minecraft.getMinecraft().getTextureMapBlocks().getMissingSprite());
    }

    @Override
    @Nonnull
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        List<BakedQuad> quads = new ArrayList<>();
        if (side == null) return quads;
        if (state != null) {
            Material material = state.getValue(((BlockFrame) state.getBlock()).variantProperty);
            materialFaces.putIfAbsent(material.getMaterialIconSet(), new EnumMap<>(EnumFacing.class));

            Map<EnumFacing, BakedQuad> materialFace = materialFaces.get(material.getMaterialIconSet());

            materialFace.putIfAbsent(side, ModelFactory.getBakery().makeBakedQuad(
                    new Vector3f(0F, 0F, 0F),
                    new Vector3f(16F, 16F, 16F),
                    new BlockPartFace(side, 1, "", new BlockFaceUV(new float[] { 0.0F, 0.0F, 16.0F, 16.0F, 0.0F, 0.0F, 16.0F, 16.0F }, 0)),
                    ModelLoader.defaultTextureGetter().apply(MaterialIconType.frameGt.getBlockTexturePath(material.getMaterialIconSet())),
                    side,
                    ModelRotation.X0_Y0,
                    null,
                    true,
                    true));

            BakedQuad materialFaceQuad = materialFace.get(side);
            quads.add(materialFaceQuad);
            particle.set(materialFaceQuad.getSprite());
        } else {
            ItemStack stack = FrameModelItemOverride.INSTANCE.stack.get();
            if (!stack.isEmpty()) {
                BlockFrame frame = (BlockFrame) ((ItemBlock) stack.getItem()).getBlock();
                IBlockState frameState = frame.getDefaultState().withProperty(frame.variantProperty, frame.variantProperty.getAllowedValues().get(stack.getMetadata()));
                for (EnumFacing face : EnumFacing.VALUES) {
                    quads.addAll(getQuads(frameState, face, rand));
                }
            }
        }
        return quads;
    }

    @Override
    public boolean isAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean isBuiltInRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return particle.get();
    }

    @Override
    public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType cameraTransformType) {
        TRSRTransformation blockTransform = ModelFactory.getBlockTransform(cameraTransformType);
        return blockTransform == null ? IBakedModel.super.handlePerspective(cameraTransformType) : Pair.of(this, blockTransform.getMatrix());
    }

    @Override
    public ItemOverrideList getOverrides() {
        return FrameModelItemOverride.INSTANCE;
    }

    private static class FrameModelItemOverride extends ItemOverrideList {

        private static final FrameModelItemOverride INSTANCE = new FrameModelItemOverride();

        private final ThreadLocal<ItemStack> stack = ThreadLocal.withInitial(() -> ItemStack.EMPTY);

        public FrameModelItemOverride() {
            super(Collections.emptyList());
        }

        @Override
        public IBakedModel handleItemState(IBakedModel originalModel, ItemStack stack, @Nullable World world, @Nullable EntityLivingBase entity) {
            this.stack.set(stack);
            return originalModel;
        }
    }
}
