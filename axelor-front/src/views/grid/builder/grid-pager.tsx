import { useEffect, useRef, useState } from "react";
import { Box, Popper, Input, Button } from "@axelor/ui";
import { i18n } from "@/services/client/i18n";
import { useSession } from "@/hooks/use-session";
import { SearchOptions, SearchPage } from "@/services/client/data";
import { MaterialIcon } from "@axelor/ui/icons/meterial-icon";
import styles from "./grid-pager.module.scss";

export function GridPager({
  page,
  onPageChange,
}: {
  page: SearchPage;
  onPageChange?: (options: Partial<SearchOptions>) => void;
}) {
  const { data: session } = useSession();
  const boxRef = useRef(null);
  const [tooltip, setTooltip] = useState<{ text: string } | null>(null);
  const [showEditor, setEditor] = useState(false);
  const [pageLimit, setPageLimit] = useState<string | number>(page.limit!);

  const start = page.offset! + 1;
  const end = Math.min(page.offset! + page.limit!, page.totalCount as number);

  function updatePageSize(value: number) {
    const MAX_PAGE_SIZE = session?.api?.pagination?.maxPerPage || 500;

    if (value > 0) {
      if (MAX_PAGE_SIZE > -1 && value > MAX_PAGE_SIZE) {
        value = MAX_PAGE_SIZE;
        setTooltip({ text: i18n.get("Display limited to {0}", value) });
      }
      setPageLimit(value);
      onPageChange && onPageChange({ limit: value });
    }

    setEditor(false);
  }

  function submit(e: React.SyntheticEvent) {
    e.preventDefault();
    e.stopPropagation();
    updatePageSize(+pageLimit || 0);
  }

  useEffect(() => {
    if (tooltip) {
      let timer = setTimeout(() => {
        setTooltip(null);
      }, 3000);

      return () => clearTimeout(timer);
    }
  }, [tooltip]);

  return (
    <Box d="flex" ref={boxRef}>
      {showEditor ? (
        <>
          <form onSubmit={submit}>
            <Input
              autoFocus
              className={styles.input}
              type="number"
              value={pageLimit}
              onChange={(e) => setPageLimit(e.target.value)}
            />
          </form>
          <Button
            d="flex"
            alignItems="center"
            ms={1}
            size="sm"
            type="submit"
            title="Ok"
            variant="light"
            onClick={submit}
          >
            <MaterialIcon icon="done" weight={300} />
          </Button>
        </>
      ) : (
        <Box as="span" className={styles.text} onClick={() => setEditor(true)}>
          {start} to {i18n.get("{0} of {1}", end, page.totalCount)}
        </Box>
      )}
      {tooltip && (
        <Popper
          open
          arrow
          target={boxRef.current}
          placement="top"
          offset={[0, 4]}
          bg="dark"
          color="light"
        >
          <Box p={2}>{tooltip.text}</Box>
        </Popper>
      )}
    </Box>
  );
}
