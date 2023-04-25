import { useEffect } from "react";
import { useTags } from "@/hooks/use-tags";

const POLL_INTERVAL = 10000;

export function NavTags() {
  const { fetchTags } = useTags();

  useEffect(() => {
    let pollPromise: NodeJS.Timeout | null = null;
    let pollIdle: NodeJS.Timeout | null = null;

    function cancelPolling() {
      if (pollPromise) {
        clearTimeout(pollPromise);
        pollPromise = null;
      }
      if (pollIdle) {
        clearTimeout(pollIdle);
        pollIdle = null;
      }
    }

    function startPolling() {
      if (pollPromise === null) {
        findTags();
      }
    }

    var pending = false;
    var pendingReset = () => {
      pollPromise = setTimeout(findTags, POLL_INTERVAL);
      if (pollIdle === null) {
        pollIdle = setTimeout(cancelPolling, POLL_INTERVAL * 2);
      }
      pending = false;
    };

    function findTags() {
      if (pending) {
        return;
      }
      if (pollPromise) {
        clearTimeout(pollPromise);
      }
      pending = true;
      fetchTags().then(pendingReset, pendingReset);
    }

    // start polling
    startPolling();

    window.addEventListener("mousemove", startPolling, false);
    window.addEventListener("mousedown", startPolling, false);
    window.addEventListener("keypress", startPolling, false);
    window.addEventListener("DOMMouseScroll", startPolling, false);
    window.addEventListener("mousewheel", startPolling, false);
    window.addEventListener("touchmove", startPolling, false);
    window.addEventListener("MSPointerMove", startPolling, false);

    return () => {
      window.removeEventListener("mousemove", startPolling, false);
      window.removeEventListener("mousedown", startPolling, false);
      window.removeEventListener("keypress", startPolling, false);
      window.removeEventListener("DOMMouseScroll", startPolling, false);
      window.removeEventListener("mousewheel", startPolling, false);
      window.removeEventListener("touchmove", startPolling, false);
      window.removeEventListener("MSPointerMove", startPolling, false);
    };
  }, [fetchTags]);

  return null;
}
