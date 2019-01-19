package nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.packets;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.entities.types.EntityType1_13;
import nl.matsv.viabackwards.api.rewriters.EntityRewriter;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.Protocol1_12_2To1_13;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.data.EntityTypeMapping;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.data.PaintingMapping;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.api.type.types.version.Types1_12;
import us.myles.ViaVersion.api.type.types.version.Types1_13;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;

import java.util.Optional;

public class EntityPackets1_13 extends EntityRewriter<Protocol1_12_2To1_13> {

	@Override
	protected void registerPackets(Protocol1_12_2To1_13 protocol) {

		//Spawn Object
		protocol.out(State.PLAY, 0x00, 0x00, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.VAR_INT);
				map(Type.UUID);
				map(Type.BYTE);
				map(Type.DOUBLE);
				map(Type.DOUBLE);
				map(Type.DOUBLE);
				map(Type.BYTE);
				map(Type.BYTE);
				map(Type.INT);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper wrapper) throws Exception {
						byte type = wrapper.get(Type.BYTE, 0);
						EntityType1_13.EntityType entityType = EntityType1_13.getTypeFromId(type, true);
						if (entityType == null) {
							ViaBackwards.getPlatform().getLogger().warning("Could not find 1.13 entity type " + type);
							return;
						}
						addTrackedEntity(
								wrapper.user(),
								wrapper.get(Type.VAR_INT, 0),
								entityType
						);
					}
				});
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper wrapper) throws Exception {
						Optional<EntityType1_13.ObjectType> type = EntityType1_13.ObjectType.findById(wrapper.get(Type.BYTE, 0));
						if (type.isPresent() && type.get() == EntityType1_13.ObjectType.FALLING_BLOCK) {
							int blockState = wrapper.get(Type.INT, 0);
							int combined = BlockItemPackets1_13.toOldId(blockState);
							combined = ((combined >> 4) & 0xFFF) | ((combined & 0xF) << 12);
							wrapper.set(Type.INT, 0, combined);
						} else if (type.isPresent() && type.get() == EntityType1_13.ObjectType.ITEM_FRAME) {
							int data = wrapper.get(Type.INT, 0);
							switch (data) {
								case 3:
									data = 0;
									break;
								case 4:
									data = 1;
									break;
								case 5:
									data = 3;
									break;
							}
							wrapper.set(Type.INT, 0, data);
						}
					}
				});
			}
		});

		//Spawn Experience Orb
		protocol.out(State.PLAY, 0x01, 0x01, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.VAR_INT);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper wrapper) throws Exception {
						addTrackedEntity(
								wrapper.user(),
								wrapper.get(Type.VAR_INT, 0),
								EntityType1_13.EntityType.XP_ORB
						);
					}
				});
			}
		});

		//Spawn Global Entity
		protocol.out(State.PLAY, 0x02, 0x02, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.VAR_INT);
				map(Type.BYTE);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper wrapper) throws Exception {
						addTrackedEntity(
								wrapper.user(),
								wrapper.get(Type.VAR_INT, 0),
								EntityType1_13.EntityType.LIGHTNING_BOLT
						);
					}
				});
			}
		});

		//Spawn Mob
		protocol.out(State.PLAY, 0x03, 0x03, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.VAR_INT);
				map(Type.UUID);
				map(Type.VAR_INT);
				map(Type.DOUBLE);
				map(Type.DOUBLE);
				map(Type.DOUBLE);
				map(Type.BYTE);
				map(Type.BYTE);
				map(Type.BYTE);
				map(Type.SHORT);
				map(Type.SHORT);
				map(Type.SHORT);
				map(Types1_13.METADATA_LIST, Types1_12.METADATA_LIST);

				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper wrapper) throws Exception {
						int type = wrapper.get(Type.VAR_INT, 1);
						EntityType1_13.EntityType entityType = EntityType1_13.getTypeFromId(type, false);
						addTrackedEntity(
								wrapper.user(),
								wrapper.get(Type.VAR_INT, 0),
								entityType
						);
						Optional<Integer> oldId = EntityTypeMapping.getOldId(type);
						if (!oldId.isPresent()) {
							ViaBackwards.getPlatform().getLogger().warning("Could not find 1.12 entity type for 1.13 entity type " + type + "/" + entityType);
							return;
						} else {
							wrapper.set(Type.VAR_INT, 1, oldId.get());
						}
					}
				});
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper wrapper) throws Exception {
						wrapper.get(Types1_12.METADATA_LIST, 0).clear();  //TODO handle metadata
					}
				});
			}
		});

		// Spawn Player
		protocol.out(State.PLAY, 0x05, 0x05, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.VAR_INT);
				map(Type.UUID);
				map(Type.DOUBLE);
				map(Type.DOUBLE);
				map(Type.DOUBLE);
				map(Type.BYTE);
				map(Type.BYTE);
				map(Types1_13.METADATA_LIST, Types1_12.METADATA_LIST);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper wrapper) throws Exception {
						addTrackedEntity(
								wrapper.user(),
								wrapper.get(Type.VAR_INT, 0),
								EntityType1_13.EntityType.PLAYER
						);
					}
				});
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper wrapper) throws Exception {
						wrapper.get(Types1_12.METADATA_LIST, 0).clear();  //TODO handle metadata
					}
				});
			}
		});

		//Spawn Painting
		protocol.out(State.PLAY, 0x04, 0x04, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.VAR_INT);
				map(Type.UUID);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper wrapper) throws Exception {
						addTrackedEntity(
								wrapper.user(),
								wrapper.get(Type.VAR_INT, 0),
								EntityType1_13.EntityType.PAINTING
						);
					}
				});
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper wrapper) throws Exception {
						int motive = wrapper.read(Type.VAR_INT);
						String title = PaintingMapping.getStringId(motive);
						wrapper.write(Type.STRING, title);
					}
				});
			}
		});

        // Join game
        protocol.out(State.PLAY, 0x25, 0x23, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // 0 - Entity ID
                map(Type.UNSIGNED_BYTE); // 1 - Gamemode
                map(Type.INT); // 2 - Dimension

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        ClientWorld clientChunks = wrapper.user().get(ClientWorld.class);
                        int dimensionId = wrapper.get(Type.INT, 1);
                        clientChunks.setEnvironment(dimensionId);
                    }
                });
            }
        });


        // Respawn Packet (save dimension id)
        protocol.registerOutgoing(State.PLAY, 0x38, 0x35, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // 0 - Dimension ID

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);
                        int dimensionId = wrapper.get(Type.INT, 0);
                        clientWorld.setEnvironment(dimensionId);
                    }
                });
            }
        });

        // Destroy Entities Packet
        protocol.registerOutgoing(State.PLAY, 0x35, 0x32, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT_ARRAY); // 0 - Entity IDS

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        for (int entity : wrapper.get(Type.VAR_INT_ARRAY, 0))
                            getEntityTracker(wrapper.user()).removeEntity(entity);
                    }
                });
            }
        });

        // Entity Metadata packet
        protocol.registerOutgoing(State.PLAY, 0x3F, 0x3C, new PacketRemapper() {
            @Override
            public void registerMap() {
                // TODO HANDLE

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        wrapper.cancel();
                    }
                });
            }
        });
    }

	@Override
	protected void registerRewrites() {

	}
}
