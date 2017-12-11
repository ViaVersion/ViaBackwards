package nl.matsv.viabackwards.protocol.protocol1_8to1_9.storage;

import nl.matsv.viabackwards.protocol.protocol1_8to1_9.Protocol1_8TO1_9;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.Pair;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.data.StoredObject;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.platform.TaskId;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.exception.CancelException;

import java.util.ArrayList;

public class Cooldown extends StoredObject {
	private double attackSpeed = 4.0;
	private long lastHit = 0;
	final TaskId taskId;

	public Cooldown(final UserConnection user) {
		super(user);

		taskId = Via.getPlatform().runRepeatingSync(new Runnable() {
			@Override
			public void run() {
				if (!user.getChannel().isOpen()) {
					Via.getPlatform().cancelTask(taskId);
					return;
				}
				if (!hasCooldown()) return;
				BlockPlaceDestroyTracker tracker = getUser().get(BlockPlaceDestroyTracker.class);
				if (tracker.isMining() || System.currentTimeMillis()-tracker.getLastMining()<50) {
					lastHit = 0;
					PacketWrapper hide = new PacketWrapper(0x45, null, getUser());
					hide.write(Type.VAR_INT, 3);
					try {
						hide.send(Protocol1_8TO1_9.class, true, false);
					} catch (CancelException ignored) {
						;
					} catch (Exception ex) {
						ex.printStackTrace();
					}
					return;
				}
				PacketWrapper time = new PacketWrapper(0x45, null, getUser());
				time.write(Type.VAR_INT, 2);
				time.write(Type.INT, 0);
				time.write(Type.INT, 2);
				time.write(Type.INT, 5);
				PacketWrapper title = new PacketWrapper(0x45, null, getUser());
				title.write(Type.VAR_INT, 0);
				title.write(Type.STRING, "");
				PacketWrapper subtitle = new PacketWrapper(0x45, null, getUser());
				subtitle.write(Type.VAR_INT, 1);
				subtitle.write(Type.STRING, getTitle());
				try {
					title.send(Protocol1_8TO1_9.class, true, true);
				} catch (CancelException ignored) {
					;
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				try {
					subtitle.send(Protocol1_8TO1_9.class, true, true);
				} catch (CancelException ignored) {
					;
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				try {
					time.send(Protocol1_8TO1_9.class, true, true);
				} catch (CancelException ignored) {
					;
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}, 1L);
	}

	public boolean hasCooldown() {
		long time = System.currentTimeMillis()-lastHit;
		double cooldown = restrain(((double)time) * attackSpeed / 1000d, 0, 1.5);
		return cooldown>0.1 && cooldown<1.2;
	}

	public double getCooldown() {
		long time = System.currentTimeMillis()-lastHit;
		return restrain(((double)time) * attackSpeed / 1000d, 0, 1);
	}

	private double restrain(double x, double a, double b) {
		if (x<a) return a;
		if (x>b) return b;
		return x;
	}

	private static final int max = 10;
	private String getTitle() {
		double cooldown = getCooldown();
		int green = (int) Math.floor(((double)max) * cooldown);
		int grey = max-green;
		StringBuilder builder = new StringBuilder("§a");
		while(green-->0) builder.append("■");
		builder.append("§7");
		while(grey-->0) builder.append("■");
		return builder.toString();
	}

	public double getAttackSpeed() {
		return attackSpeed;
	}

	public void setAttackSpeed(double attackSpeed) {
		this.attackSpeed = attackSpeed;
	}

	public void setAttackSpeed(double base, ArrayList<Pair<Byte, Double>> modifiers) {
		attackSpeed = base;
		for (int j = 0; j<modifiers.size(); j++) {
			if (modifiers.get(j).getKey()==0) {
				attackSpeed += modifiers.get(j).getValue();
				modifiers.remove(j--);
			}
		}
		for (int j = 0; j<modifiers.size(); j++) {
			if (modifiers.get(j).getKey()==1) {
				attackSpeed += base * modifiers.get(j).getValue();
				modifiers.remove(j--);
			}
		}
		for (int j = 0; j<modifiers.size(); j++) {
			if (modifiers.get(j).getKey()==2) {
				attackSpeed *= (1.0 + modifiers.get(j).getValue());
				modifiers.remove(j--);
			}
		}
	}

	public void hit() {
		BlockPlaceDestroyTracker tracker = getUser().get(BlockPlaceDestroyTracker.class);
		if (tracker.isMining() || System.currentTimeMillis()-tracker.getBlockPlaced()<100 || System.currentTimeMillis()-tracker.getLastMining()<100) return;
		lastHit = System.currentTimeMillis();
	}

	public void setLastHit(long lastHit) {
		this.lastHit = lastHit;
	}
}
