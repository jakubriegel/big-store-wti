if fc-list | grep -q "JetBrains Mono Medium"
then
    dot -Tpng -obig-store-schema.png big-store.gv
    dot -Tpng -obig-store-inflow-schema.png big-store-data-inflow.gv
    dot -Tpng -obig-store-outflow-schema.png big-store-data-outflow.gv
    exit 0
else
    echo "To generate the schema get JetBrains Mono typeface from https://www.jetbrains.com/lp/mono/"
    exit 1
fi
