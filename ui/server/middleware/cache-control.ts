export default defineEventHandler(event => {
    const res = event.node.res;
    const url = event.node.req.url;
    if (url) {
        res.setHeader('Cache-Control', `public, max-age=300`);
    }
});
