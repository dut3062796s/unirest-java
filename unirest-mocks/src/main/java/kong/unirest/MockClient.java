/**
 * The MIT License
 *
 * Copyright for portions of unirest-java are held by Kong Inc (c) 2013.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package kong.unirest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * A Mock client for unirest to make requests against
 * This implements both sync and async clients
 */
public class MockClient implements Client, AsyncClient {
    private List<Paths> paths = new ArrayList<>();

    @Override
    public <T> HttpResponse<T> request(HttpRequest request, Function<RawResponse, HttpResponse<T>> transformer) {
        Paths exp = findExpecation(request);
        RawResponse response = exp.exchange(request);
        return transformer.apply(response);
    }

    private Paths findExpecation(HttpRequest request) {
        return paths.stream()
                .filter(e -> e.matches(request))
                .findFirst()
                .orElseGet(() -> createNewPath(request));
    }

    private Paths createNewPath(HttpRequest request) {
        Paths p = new Paths(request);
        paths.add(p);
        return p;
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> request(HttpRequest request, Function<RawResponse, HttpResponse<T>> transformer, CompletableFuture<HttpResponse<T>> callback) {
        return null;
    }

    @Override
    public Object getClient() {
        return this;
    }

    @Override
    public Stream<Exception> close() {
        return Stream.empty();
    }

    @Override
    public void registerShutdownHook() {

    }

    public Expectation expect(HttpMethod method, String path) {
        Paths exp = findByPath(method, path).orElseGet(() -> new Paths(method, path));
        if(!this.paths.contains(exp)) {
            this.paths.add(exp);
        }
        return exp.newExpectation();
    }

    public Assert assertThat(HttpMethod get, String path) {
        return findByPath(get, path)
                .orElseThrow(() -> new UnirestAssertion(String.format("No Matching Invocation:: %s %s", get, path)));
    }

    private Optional<Paths> findByPath(HttpMethod get, String path) {
        return paths.stream()
                    .filter(e -> e.matches(get, path))
                    .findFirst();
    }

    public void verifyAll() {
        paths.forEach(e -> e.verifyAll());
    }
}