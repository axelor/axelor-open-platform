import { MutableRefObject } from "react";

import { device } from "@/utils/device";
import { useContainerQuery } from "../use-container-query";
import { useMediaQuery } from "../use-media-query";

// see https://getbootstrap.com/docs/5.3/layout/breakpoints/
export function useResponsive() {
  const xs = useMediaQuery("(max-width: 575.98px)");
  const sm = useMediaQuery("(min-width: 576px) and (max-width: 767.98px)");
  const md = useMediaQuery("(min-width: 768px) and (max-width: 991.98px)");
  const lg = useMediaQuery("(min-width: 992px) and (max-width: 1199.98px)");
  const xl = useMediaQuery("(min-width: 1200px) and (max-width: 1399.98px)");
  const xxl = useMediaQuery("(min-width: 1400px)");
  return {
    xs,
    sm,
    md,
    lg,
    xl,
    xxl,
  };
}

export function useResponsiveContainer(
  ref: MutableRefObject<HTMLElement | null>
) {
  const xs = useContainerQuery(ref, "width < 576px");
  const sm = useContainerQuery(ref, "width < 768px");
  const md = useContainerQuery(ref, "width < 992px");
  const lg = useContainerQuery(ref, "width < 1200px");
  const xl = useContainerQuery(ref, "width < 1400px");
  const xxl = useContainerQuery(ref, "width > 1400px");
  return {
    xs,
    sm: sm && !xs,
    md: md && !sm,
    lg: lg && !md,
    xl: xl && !lg,
    xxl: xxl && !xl,
  };
}

export function useDevice() {
  return device;
}
