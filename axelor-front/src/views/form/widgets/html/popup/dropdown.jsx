import { cancelEvent } from '../utils';

function Link(props) {
  return (
    <a unselectable="on" alt="Action" {...props}>
      {props.children}
    </a>
  );
}

function DropdownPopup({ commands, data, onClick }) {
  return (
    <div className="custom-html-editor-plugin-list" unselectable="on">
      {Object.keys(data || {}).map(key => (
        <Link
          key={key}
          onClick={e => {
            cancelEvent(e);
            onClick && onClick(commands, key);
          }}
        >
          {data[key]}
        </Link>
      ))}
    </div>
  );
}
export default DropdownPopup;
