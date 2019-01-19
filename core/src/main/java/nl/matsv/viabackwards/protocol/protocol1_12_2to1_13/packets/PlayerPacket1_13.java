package nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.packets;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.rewriters.Rewriter;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.Protocol1_12_2To1_13;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.minecraft.Position;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.ChatRewriter;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.packets.InventoryPackets;

public class PlayerPacket1_13 extends Rewriter<Protocol1_12_2To1_13> {
	@Override
	protected void registerPackets(Protocol1_12_2To1_13 protocol) {

		//Plugin Message
		protocol.out(State.PLAY, 0x19, 0x18, new PacketRemapper() {
			@Override
			public void registerMap() {
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper wrapper) throws Exception {
						String channel = wrapper.read(Type.STRING);
						if (channel.equals("minecraft:trader_list")) {
							wrapper.write(Type.STRING, "MC|TrList");
							wrapper.passthrough(Type.INT); //Passthrough Window ID

							int size = wrapper.passthrough(Type.UNSIGNED_BYTE);
							for (int i = 0; i < size; i++) {
								//Input Item
								Item input = wrapper.read(Type.FLAT_ITEM);
								BlockItemPackets1_13.toClient(input);
								wrapper.write(Type.ITEM, input);
								//Output Item
								Item output = wrapper.read(Type.FLAT_ITEM);
								BlockItemPackets1_13.toClient(output);
								wrapper.write(Type.ITEM, output);

								boolean secondItem = wrapper.passthrough(Type.BOOLEAN); //Has second item
								if (secondItem) {
									//Second Item
									Item second = wrapper.read(Type.FLAT_ITEM);
									BlockItemPackets1_13.toClient(second);
									wrapper.write(Type.ITEM, second);
								}

								wrapper.passthrough(Type.BOOLEAN); //Trade disabled
								wrapper.passthrough(Type.INT); //Number of tools uses
								wrapper.passthrough(Type.INT); //Maximum number of trade uses
							}
						} else {
							String oldChannel = InventoryPackets.getOldPluginChannelId(channel);
							if (oldChannel == null) {
								ViaBackwards.getPlatform().getLogger().warning("Could not find old channel for " + channel);
								wrapper.cancel();
								return;
							}
							wrapper.write(Type.STRING, oldChannel);
						}
					}
				});
			}
		});

		//Scoreboard Objective
		protocol.out(State.PLAY, 0x45, 0x42, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.STRING);
				map(Type.BYTE);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper wrapper) throws Exception {
						byte mode = wrapper.get(Type.BYTE, 0);
						if (mode == 0 || mode == 2) {
							String value = wrapper.read(Type.STRING);
							value = ChatRewriter.jsonTextToLegacy(value);
							if (value.length() > 32) value = value.substring(0, 32);
							wrapper.write(Type.STRING, value);
							int type = wrapper.read(Type.VAR_INT);
							wrapper.write(Type.STRING, type == 1 ? "hearts" : "integer");
						}
					}
				});
			}
		});

		//Teams
		protocol.out(State.PLAY, 0x47, 0x44, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.STRING);
				map(Type.BYTE);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper wrapper) throws Exception {
						byte action = wrapper.get(Type.BYTE, 0);
						if (action == 0 || action == 2) {
							String displayName = wrapper.read(Type.STRING);
							displayName = ChatRewriter.jsonTextToLegacy(displayName);
							if (displayName.length() > 32) displayName = displayName.substring(0, 32);
							wrapper.write(Type.STRING, displayName);

							String prefix = wrapper.read(Type.STRING);
							String suffix = wrapper.read(Type.STRING);

							wrapper.passthrough(Type.BYTE); //Flags

							wrapper.passthrough(Type.STRING); //Name Tag Visibility
							wrapper.passthrough(Type.STRING); //Collision Rule

							int colour = wrapper.read(Type.VAR_INT);
							if (colour == 21) {
								colour = -1;
							}

							wrapper.write(Type.BYTE, (byte) colour);

							wrapper.write(Type.STRING, ChatRewriter.jsonTextToLegacy(prefix));
							wrapper.write(Type.STRING, ChatRewriter.jsonTextToLegacy(suffix));
						}

						if (action == 0 || action == 3 || action == 4) {
							wrapper.passthrough(Type.STRING_ARRAY); //Entities
						}
					}
				});
			}
		});

		//Plugin Message
		protocol.in(State.PLAY, 0x0A, 0x09, new PacketRemapper() {
			@Override
			public void registerMap() {
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper wrapper) throws Exception {
						String channel = wrapper.read(Type.STRING);
						if (channel.equals("MC|BSign") || channel.equals("MC|BEdit")) {
							wrapper.setId(0x0B);
							Item book = wrapper.read(Type.ITEM);
							System.out.println(book);
							BlockItemPackets1_13.toServer(book);
							System.out.println(book);
							wrapper.write(Type.FLAT_ITEM, book);
							boolean signing = channel.equals("MC|BSign");
							System.out.println(channel);
							System.out.println(signing);
							wrapper.write(Type.BOOLEAN, signing);
						} else if (channel.equals("MC|ItemName")) {
							wrapper.setId(0x1C);
						} else if (channel.equals("MC|AdvCmd")) {
							byte type = wrapper.read(Type.BYTE);
							if (type == 0) {
								//Information from https://wiki.vg/index.php?title=Plugin_channels&oldid=14089
								//The Notchain client only uses this for command block minecarts and uses MC|AutoCmd for blocks, but the Notchian server still accepts it for either.
								//Maybe older versions used this and we need to implement this? The issues is that we would have to save the command block types
								wrapper.setId(0x22);
								wrapper.cancel();
								ViaBackwards.getPlatform().getLogger().warning("Client send MC|AdvCmd custom payload to update command block, weird!");
							} else if (type == 1) {
								wrapper.setId(0x23);
								wrapper.write(Type.VAR_INT, wrapper.read(Type.INT)); //Entity Id
								wrapper.passthrough(Type.STRING); //Command
								wrapper.passthrough(Type.BOOLEAN); //Track Output

							} else {
								wrapper.cancel();
							}
						} else if (channel.equals("MC|AutoCmd")) {
							wrapper.setId(0x22);
							Integer x = wrapper.read(Type.INT);
							Integer y = wrapper.read(Type.INT);
							Integer z = wrapper.read(Type.INT);
							wrapper.write(Type.POSITION, new Position(x.longValue(), y.longValue(), z.longValue()));
							wrapper.passthrough(Type.STRING);  //Command
							byte flags = 0;
							if (wrapper.read(Type.BOOLEAN)) flags |= 0x01; //Track Output
							String mode = wrapper.read(Type.STRING);
							int modeId = mode.equals("SEQUENCE") ? 0 : mode.equals("AUTO") ? 1 : 2;
							wrapper.write(Type.VAR_INT, modeId);
							if (wrapper.read(Type.BOOLEAN)) flags |= 0x02; //Is conditional
							if (wrapper.read(Type.BOOLEAN)) flags |= 0x04; //Automatic
						} else if (channel.equals("MC|Struct")) {
							wrapper.setId(0x25);
							Integer x = wrapper.read(Type.INT);
							Integer y = wrapper.read(Type.INT);
							Integer z = wrapper.read(Type.INT);
							wrapper.write(Type.POSITION, new Position(x.longValue(), y.longValue(), z.longValue()));
							wrapper.write(Type.VAR_INT, wrapper.read(Type.BYTE) - 1);
							String mode = wrapper.read(Type.STRING);
							int modeId = mode.equals("SAVE") ? 0 : mode.equals("LOAD") ? 1 : mode.equals("CORNER") ? 2 : 3;
							wrapper.write(Type.VAR_INT, modeId);
							wrapper.passthrough(Type.STRING); //Name
							wrapper.write(Type.BYTE, wrapper.read(Type.INT).byteValue()); //Offset X
							wrapper.write(Type.BYTE, wrapper.read(Type.INT).byteValue()); //Offset Y
							wrapper.write(Type.BYTE, wrapper.read(Type.INT).byteValue()); //Offset Z
							wrapper.write(Type.BYTE, wrapper.read(Type.INT).byteValue()); //Size X
							wrapper.write(Type.BYTE, wrapper.read(Type.INT).byteValue()); //Size Y
							wrapper.write(Type.BYTE, wrapper.read(Type.INT).byteValue()); //Size Z
							String mirror = wrapper.read(Type.STRING);
							int mirrorId = mode.equals("NONE") ? 0 : mode.equals("LEFT_RIGHT") ? 1 : 2;
							String rotation = wrapper.read(Type.STRING);
							int rotationId = mode.equals("NONE") ? 0 : mode.equals("CLOCKWISE_90") ? 1 : mode.equals("CLOCKWISE_180") ? 2 : 3;
							wrapper.passthrough(Type.STRING); //Metadata
							byte flags = 0;
							if (wrapper.read(Type.BOOLEAN)) flags |= 0x01; //Ignore entities
							if (wrapper.read(Type.BOOLEAN)) flags |= 0x02; //Show air
							if (wrapper.read(Type.BOOLEAN)) flags |= 0x04; //Show bounding box
							wrapper.passthrough(Type.FLOAT); //Integrity
							wrapper.passthrough(Type.VAR_LONG); //Seed
							wrapper.write(Type.BYTE, flags);
						} else if (channel.equals("MC|Beacon")) {
							wrapper.setId(0x20);
							wrapper.write(Type.VAR_INT, wrapper.read(Type.INT)); //Primary Effect
							wrapper.write(Type.VAR_INT, wrapper.read(Type.INT)); //Secondary Effect
						} else if (channel.equals("MC|TrSel")) {
							wrapper.setId(0x1F);
							wrapper.write(Type.VAR_INT, wrapper.read(Type.INT)); //Slot
						} else if (channel.equals("MC|PickItem")) {
							wrapper.setId(0x15);
						} else {
							String newChannel = InventoryPackets.getNewPluginChannelId(channel);
							if (newChannel == null) {
								ViaBackwards.getPlatform().getLogger().warning("Could not find new channel for " + channel);
								wrapper.cancel();
								return;
							}
							wrapper.write(Type.STRING, newChannel);
							//TODO REGISTER and UNREGISTER (see ViaVersion)
							wrapper.cancel();
						}
					}
				});
			}
		});
	}

	@Override
	protected void registerRewrites() {

	}
}
