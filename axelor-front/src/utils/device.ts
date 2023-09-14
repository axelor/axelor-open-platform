const ua = navigator.userAgent;

const isMobile = /Mobile|Android|iPhone|iPad/i.test(ua);
const isTablet = /iPad|Tablet/i.test(ua);
const isDesktop = !isMobile && !isTablet;
const isMac = /(Mac OS)|(Macintosh)/i.test(ua);
const isLinux = /(Linux|CrOS)/i.test(ua);
const isWindows = /Windows/i.test(ua);

export type DeviceInfo = {
  isMobile: boolean;
  isTablet: boolean;
  isDesktop: boolean;
  isMac: boolean;
  isLinux: boolean;
  isWindows: boolean;
};

export const device: DeviceInfo = {
  isMobile,
  isTablet,
  isDesktop,
  isMac,
  isLinux,
  isWindows,
};
