import { useMomentLocale } from "@/hooks/use-moment-locale";
import { Scheduler, SchedulerProps } from "@axelor/ui/scheduler";

function LocaleScheduler(props: SchedulerProps) {
  const { momentLocale } = useMomentLocale();
  return momentLocale ? <Scheduler {...props} /> : null;
}

export default LocaleScheduler;
