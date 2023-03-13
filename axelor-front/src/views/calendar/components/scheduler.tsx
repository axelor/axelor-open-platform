import { useMomentLocale } from "@/hooks/use-moment-locale";
import { Scheduler, SchedulerProps } from "@axelor/ui/scheduler";
import Loading from "./loading";

function LocaleScheduler(props: SchedulerProps) {
  const momentLocale = useMomentLocale();
  return momentLocale ? <Scheduler {...props} /> : <Loading />;
}

export default LocaleScheduler;
