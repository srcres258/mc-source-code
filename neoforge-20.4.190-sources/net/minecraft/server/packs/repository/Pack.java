package net.minecraft.server.packs.repository;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.packs.FeatureFlagsMetadataSection;
import net.minecraft.server.packs.OverlayMetadataSection;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.util.InclusiveRange;
import net.minecraft.world.flag.FeatureFlagSet;
import org.slf4j.Logger;

public class Pack {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final String id;
    private final Pack.ResourcesSupplier resources;
    private final Component title;
    private final Pack.Info info;
    private final Pack.Position defaultPosition;
    private final boolean required;
    private final boolean fixedPosition;
    private final boolean hidden; // Neo: Allow packs to be hidden from the UI entirely
    private final List<Pack> children; // Neo: Allows packs to specify packs which will always be placed beneath them; must be hidden
    private final PackSource packSource;

    @Nullable
    public static Pack readMetaAndCreate(
        String pId,
        Component pTitle,
        boolean pRequired,
        Pack.ResourcesSupplier pResources,
        PackType pPackType,
        Pack.Position pDefaultPosition,
        PackSource pPackSource
    ) {
        int i = SharedConstants.getCurrentVersion().getPackVersion(pPackType);
        Pack.Info pack$info = readPackInfo(pId, pResources, i);
        return pack$info != null ? create(pId, pTitle, pRequired, pResources, pack$info, pDefaultPosition, false, pPackSource) : null;
    }

    public static Pack create(
        String pId,
        Component pTitle,
        boolean pRequired,
        Pack.ResourcesSupplier pResources,
        Pack.Info pInfo,
        Pack.Position pDefaultPosition,
        boolean pFixedPosition,
        PackSource pPackSource
    ) {
        return new Pack(pId, pRequired, pResources, pTitle, pInfo, pDefaultPosition, pFixedPosition, pPackSource);
    }

    private Pack(
        String pId,
        boolean pRequired,
        Pack.ResourcesSupplier pResources,
        Component pTitle,
        Pack.Info pInfo,
        Pack.Position pDefaultPosition,
        boolean pFixedPosition,
        PackSource pPackSource
    ) {
        this(pId, pRequired, pResources, pTitle, pInfo, pDefaultPosition, pFixedPosition, pPackSource, List.of());
    }

    private Pack(
        String p_252218_,
        boolean p_248829_,
        Pack.ResourcesSupplier p_249377_,
        Component p_251718_,
        Pack.Info p_250162_,
        Pack.Position p_251298_,
        boolean p_249753_,
        PackSource p_251608_,
        List<Pack> children
    ) {
        List<Pack> flattenedChildren = new java.util.ArrayList<>();
        List<Pack> remainingChildren = children;
        // recursively flatten children
        while (!remainingChildren.isEmpty()) {
            List<Pack> oldChildren = remainingChildren;
            remainingChildren = new java.util.ArrayList<>();
            for (Pack child : oldChildren) {
                flattenedChildren.add(child.withChildren(List.of()).hidden());
                remainingChildren.addAll(child.getChildren());
            }
        }
        this.children = List.copyOf(flattenedChildren);
        this.hidden = p_250162_.isHidden();
        this.id = p_252218_;
        this.resources = p_249377_;
        this.title = p_251718_;
        this.info = p_250162_;
        this.required = p_248829_;
        this.defaultPosition = p_251298_;
        this.fixedPosition = p_249753_;
        this.packSource = p_251608_;
    }

    @Nullable
    public static Pack.Info readPackInfo(String pId, Pack.ResourcesSupplier pResources, int pPackVersion) {
        try {
            Pack.Info pack$info;
            try (PackResources packresources = pResources.openPrimary(pId)) {
                PackMetadataSection packmetadatasection = packresources.getMetadataSection(PackMetadataSection.TYPE);
                if (packmetadatasection == null) {
                    LOGGER.warn("Missing metadata in pack {}", pId);
                    return null;
                }

                FeatureFlagsMetadataSection featureflagsmetadatasection = packresources.getMetadataSection(FeatureFlagsMetadataSection.TYPE);
                FeatureFlagSet featureflagset = featureflagsmetadatasection != null ? featureflagsmetadatasection.flags() : FeatureFlagSet.of();
                InclusiveRange<Integer> inclusiverange = getDeclaredPackVersions(pId, packmetadatasection);
                PackCompatibility packcompatibility = PackCompatibility.forVersion(inclusiverange, pPackVersion);
                OverlayMetadataSection overlaymetadatasection = packresources.getMetadataSection(OverlayMetadataSection.TYPE);
                List<String> list = overlaymetadatasection != null ? overlaymetadatasection.overlaysForVersion(pPackVersion) : List.of();
                pack$info = new Pack.Info(packmetadatasection.description(), packcompatibility, featureflagset, list, packresources.isHidden());
            }

            return pack$info;
        } catch (Exception exception) {
            LOGGER.warn("Failed to read pack {} metadata", pId, exception);
            return null;
        }
    }

    public static InclusiveRange<Integer> getDeclaredPackVersions(String pId, PackMetadataSection pMetadata) {
        int i = pMetadata.packFormat();
        if (pMetadata.supportedFormats().isEmpty()) {
            return new InclusiveRange<>(i);
        } else {
            InclusiveRange<Integer> inclusiverange = pMetadata.supportedFormats().get();
            if (!inclusiverange.isValueInRange(i)) {
                LOGGER.warn("Pack {} declared support for versions {} but declared main format is {}, defaulting to {}", pId, inclusiverange, i, i);
                return new InclusiveRange<>(i);
            } else {
                return inclusiverange;
            }
        }
    }

    public Component getTitle() {
        return this.title;
    }

    public Component getDescription() {
        return this.info.description();
    }

    /**
     * @param pGreen used to indicate either a successful operation or datapack
     *               enabled status
     */
    public Component getChatLink(boolean pGreen) {
        return ComponentUtils.wrapInSquareBrackets(this.packSource.decorate(Component.literal(this.id)))
            .withStyle(
                p_293812_ -> p_293812_.withColor(pGreen ? ChatFormatting.GREEN : ChatFormatting.RED)
                        .withInsertion(StringArgumentType.escapeIfRequired(this.id))
                        .withHoverEvent(
                            new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.empty().append(this.title).append("\n").append(this.info.description))
                        )
            );
    }

    public PackCompatibility getCompatibility() {
        return this.info.compatibility();
    }

    public FeatureFlagSet getRequestedFeatures() {
        return this.info.requestedFeatures();
    }

    public PackResources open() {
        return this.resources.openFull(this.id, this.info);
    }

    public String getId() {
        return this.id;
    }

    public boolean isRequired() {
        return this.required;
    }

    public boolean isFixedPosition() {
        return this.fixedPosition;
    }

    public Pack.Position getDefaultPosition() {
        return this.defaultPosition;
    }

    public PackSource getPackSource() {
        return this.packSource;
    }

    public boolean isHidden() { return hidden; }

    public List<Pack> getChildren() { return children; }

    public java.util.stream.Stream<Pack> streamSelfAndChildren() {
        return java.util.stream.Stream.concat(java.util.stream.Stream.of(this), children.stream());
    }

    /**
     * {@return a copy of the pack with the provided children in place of any children this pack currently has}
     */
    public Pack withChildren(List<Pack> children) {
        return new Pack(this.id, this.required, this.resources, this.title, this.info, this.defaultPosition, this.fixedPosition, this.packSource, children);
    }

    /**
     * {@return a copy of the pack that is hidden}
     */
    public Pack hidden() {
        return new Pack(
                this.id, this.required, this.resources, this.title,
                new Info(
                        this.info.description(),
                        info.compatibility(),
                        info.requestedFeatures(),
                        info.overlays(),
                        true
                ),
                this.defaultPosition, this.fixedPosition, this.packSource, this.children);
    }

    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) {
            return true;
        } else if (!(pOther instanceof Pack)) {
            return false;
        } else {
            Pack pack = (Pack)pOther;
            return this.id.equals(pack.id);
        }
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    public static record Info(Component description, PackCompatibility compatibility, FeatureFlagSet requestedFeatures, List<String> overlays, boolean isHidden) {
    }

    public static enum Position {
        TOP,
        BOTTOM;

        public <T> int insert(List<T> pList, T pElement, Function<T, Pack> pPackFactory, boolean pFlipPosition) {
            Pack.Position pack$position = pFlipPosition ? this.opposite() : this;
            if (pack$position == BOTTOM) {
                int j;
                for(j = 0; j < pList.size(); ++j) {
                    Pack pack1 = pPackFactory.apply(pList.get(j));
                    if (!pack1.isFixedPosition() || pack1.getDefaultPosition() != this) {
                        break;
                    }
                }

                pList.add(j, pElement);
                return j;
            } else {
                int i;
                for(i = pList.size() - 1; i >= 0; --i) {
                    Pack pack = pPackFactory.apply(pList.get(i));
                    if (!pack.isFixedPosition() || pack.getDefaultPosition() != this) {
                        break;
                    }
                }

                pList.add(i + 1, pElement);
                return i + 1;
            }
        }

        public Pack.Position opposite() {
            return this == TOP ? BOTTOM : TOP;
        }
    }

    public interface ResourcesSupplier {
        PackResources openPrimary(String pId);

        PackResources openFull(String pId, Pack.Info pInfo);
    }
}
