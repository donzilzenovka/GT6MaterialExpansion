package zenovka.gt6materialexpansion;

import cpw.mods.fml.common.FMLLog;
import net.minecraft.item.ItemStack;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Temporary diagnostics: detects unexpected GT6 sound enqueues during recipe inspection. */
public final class SoundQueueDiagnostic {
    private static final AtomicInteger EVENTS = new AtomicInteger();
    private static volatile Field queueField;
    private static volatile boolean lookupAttempted;
    private SoundQueueDiagnostic() {}

    public static int pending() {
        try {
            if (!lookupAttempted) {
                synchronized (SoundQueueDiagnostic.class) {
                    if (!lookupAttempted) {
                        Class<?> sounds = Class.forName("gregapi.util.UT$Sounds");
                        Field field = sounds.getDeclaredField("sSoundsToPlay");
                        field.setAccessible(true);
                        queueField = field;
                        lookupAttempted = true;
                    }
                }
            }
            Object value = queueField == null ? null : queueField.get(null);
            // Read-only, best-effort measurement; a concurrent queue mutation may race with size().
            return value instanceof List ? ((List<?>) value).size() : -1;
        } catch (Throwable failure) {
            if (EVENTS.getAndIncrement() == 0)
                FMLLog.warning("[GT6MaterialExpansion][sound-trace] Cannot inspect GT6 queue: %s", failure);
            return -1;
        }
    }

    public static void report(String operation, ItemStack stack, int before) {
        int after = pending();
        if (before < 0 || after <= before) return;
        if (EVENTS.incrementAndGet() > 60) return;
        String item = stack == null || stack.getItem() == null ? "null"
                : String.valueOf(net.minecraft.item.Item.itemRegistry.getNameForObject(stack.getItem()))
                  + ":" + stack.getItemDamage();
        FMLLog.warning("[GT6MaterialExpansion][sound-trace] QUEUE GREW operation=%s item=%s before=%d after=%d thread=%s",
                operation, item, before, after, Thread.currentThread().getName());
        for (StackTraceElement frame : Thread.currentThread().getStackTrace())
            FMLLog.warning("[GT6MaterialExpansion][sound-trace]   at %s", frame);
    }
}
