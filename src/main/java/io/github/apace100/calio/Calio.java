package io.github.apace100.calio;

import io.github.apace100.calio.data.SerializableDataTypes;
import io.github.apace100.calio.mixin.CriteriaRegistryInvoker;
import io.github.apace100.calio.util.TagManagerGetter;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.MessageType;
import net.minecraft.tag.Tag;
import net.minecraft.tag.TagGroup;
import net.minecraft.tag.TagManager;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

public class Calio implements ModInitializer {

	public static final Identifier PACKET_SHARE_ITEM = new Identifier("calio", "share_item");

	static TagManagerGetter tagManagerGetter;

	@Override
	public void onInitialize() {
		CriteriaRegistryInvoker.callRegister(CodeTriggerCriterion.INSTANCE);
		ServerPlayNetworking.registerGlobalReceiver(PACKET_SHARE_ITEM, ((minecraftServer, serverPlayerEntity, serverPlayNetworkHandler, packetByteBuf, packetSender) -> {
			ItemStack stack = SerializableDataTypes.ITEM_STACK.receive(packetByteBuf);
			minecraftServer.execute(() -> {
				Text chatText = new TranslatableText("chat.type.text", serverPlayerEntity.getDisplayName(), stack.toHoverableText());
				minecraftServer.getPlayerManager().broadcastChatMessage(chatText, MessageType.CHAT, serverPlayerEntity.getUuid());
			});
		}));
	}

	public static boolean hasNonItalicName(ItemStack stack) {
		return stack.hasTag() && stack.getSubTag("display") != null && stack.getSubTag("display").getBoolean(NbtConstants.NON_ITALIC_NAME);
	}

	public static void setNameNonItalic(ItemStack stack) {
		if(stack != null)
			stack.getOrCreateSubTag("display").putBoolean(NbtConstants.NON_ITALIC_NAME, true);
	}

	public static boolean areEntityAttributesAdditional(ItemStack stack) {
		return stack.hasTag() && stack.getTag().contains(NbtConstants.ADDITIONAL_ATTRIBUTES) && stack.getTag().getBoolean(NbtConstants.ADDITIONAL_ATTRIBUTES);
	}

	/**
	 * Sets whether the item stack counts the entity attribute modifiers specified in its tag as additional,
	 * meaning they won't overwrite the equipment's inherent modifiers.
	 * @param stack
	 * @param additional
	 */
	public static void setEntityAttributesAdditional(ItemStack stack, boolean additional) {
		if(stack != null) {
			if(additional) {
				stack.getOrCreateTag().putBoolean(NbtConstants.ADDITIONAL_ATTRIBUTES, true);
			} else {
				if(stack.hasTag()) {
					stack.getTag().remove(NbtConstants.ADDITIONAL_ATTRIBUTES);
				}
			}
		}
	}

	public static TagManager getTagManager() {
		return tagManagerGetter.get();
	}

	@Nullable
	public static <T> TagGroup<T> getTagGroup(RegistryKey<? extends Registry<T>> registryKey) {
		if (registryKey.equals(Registry.ITEM_KEY)) {
			return (TagGroup<T>) Calio.getTagManager().getItems();
		}
		if (registryKey.equals(Registry.FLUID_KEY)) {
			return (TagGroup<T>) Calio.getTagManager().getFluids();
		}
		if (registryKey.equals(Registry.BLOCK_KEY)) {
			return (TagGroup<T>) Calio.getTagManager().getBlocks();
		}
		if (registryKey.equals(Registry.ENTITY_TYPE_KEY)) {
			return (TagGroup<T>) Calio.getTagManager().getEntityTypes();
		}
		return null;
	}

	public static <T, E extends Exception> Identifier getTagId(RegistryKey<? extends Registry<T>> registryKey, Tag<T> tag, Supplier<E> exceptionSupplier) throws E {
		TagGroup<T> tagGroup = Calio.getTagGroup(registryKey);
		if (tagGroup == null) {
			throw (E) exceptionSupplier.get();
		} else {
			Identifier identifier = tagGroup.getUncheckedTagId(tag);
			if (identifier == null) {
				throw (E) exceptionSupplier.get();
			} else {
				return identifier;
			}
		}
	}

	public static <T, E extends Exception> Tag<T> getTag(RegistryKey<? extends Registry<T>> registryKey, Identifier id, Function<Identifier, E> exceptionSupplier) throws E {
		TagGroup<T> tagGroup = Calio.getTagGroup(registryKey);
		if (tagGroup == null) {
			throw (E) exceptionSupplier.apply(id);
		} else {
			Tag<T> tag = tagGroup.getTag(id);
			if (tag == null) {
				throw (E) exceptionSupplier.apply(id);
			} else {
				return tag;
			}
		}
	}

	public static <T> boolean areTagsEqual(RegistryKey<? extends Registry<T>> registryKey, Tag<T> tag1, Tag<T> tag2) {
		if(tag1 == tag2) {
			return true;
		}
		if(tag1 == null || tag2 == null) {
			return false;
		}
		TagManager tagManager = Calio.getTagManager();
		try {
			Identifier id1;
			if(tag1 instanceof Tag.Identified) {
				id1 = ((Tag.Identified)tag1).getId();
			} else {
				id1 = Calio.getTagId(registryKey, tag1, RuntimeException::new);
			}
			Identifier id2;
			if(tag2 instanceof Tag.Identified) {
				id2 = ((Tag.Identified)tag2).getId();
			} else {
				id2 = Calio.getTagId(registryKey, tag2, RuntimeException::new);
			}
			return id1.equals(id2);
		} catch (Exception e) {
			return false;
		}
	}
}
