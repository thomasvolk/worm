#!/usr/bin/env groovy
@GrabResolver(name='jitpack', root='https://jitpack.io')
@Grapes([
  @Grab(group='com.github.thomasvolk', module='worm', version='-SNAPSHOT'),
  @Grab(group='org.jetbrains.kotlin', module='kotlin-stdlib', version='1.1.3')
])
import net.t53k.worm.*
import org.jsoup.Jsoup

def cli = new CliBuilder(usage: 'onePage.groovy -[tw] <url> <outfile>')
cli.with {
  t longOpt: 'timeout', args: 1, required: false, 'timeout in ms (default: 2000)'
  w longOpt: 'worker', args: 1, required: false, 'worker count (default: 1)'
  s longOpt: 'selector', args: 1, required: false, "html selector (defualt: 'html body')"
  e longOpt: 'extension', args: 1, required: false, 'file extension (default: html)'
}
def options = cli.parse(args)
def extraArguments = options.arguments()

if(extraArguments.size() < 2) {
  cli.usage()
  System.exit(1)
}

def timeout = (options.t ?: 2000) as Long
def worker = (options.w ?: 1) as Integer
def selector = options.s ?: 'html body'
def extension = options.e ?: 'html'

def outfile = extraArguments[1]
def seed = new URI(extraArguments[0])
def base = "${seed.scheme}://${seed.host}"

println """
seed: $seed
base: $base
extension: $extension
timeout: $timeout ms
worker: $worker
selector: $selector
outfile: $outfile
"""

class OnePage {
  def selector
  def root
  def documents = [:]
  def addDocument(doc) {
    if(doc.resource.body.contentType.mimeType.startsWith('text/html')) {
      def url = doc.resource.url
      if(!root) { root = doc }
      println "ADD document: $url"
      documents[url] = [doc: doc, title: getTitle(doc), uuid: UUID.randomUUID().toString() ]
    } else { println "  - skip $doc.resource.url" }
  }

  private parse(doc) {
    Jsoup.parse(new String(doc.resource.body.content, 'UTF-8'))
  }

  private unwrap(doc) {
    parse(doc).select(selector).html()
  }

  private getDocumentBodies() {
    documents.values().collect { """<a name="$it.uuid">$it.title</a>\n""" + unwrap(it.doc) }.join("\n<!-- ------------------- -->\n")
  }

  private getTitle(doc) {
    parse(doc).select('html head title').text()
  }

  private getTitle() {
    root ? getTitle(root) : ''
  }

  private getIndex() {
    "<h2>INDEX</h2><ul>" + documents.values().collect { """<li><a href="#$it.uuid">$it.title</a></li>""" }.join('\n') + "</ul>"
  }

  String toString() {
    """
<html>
  <head>
    <title>$title</title>
  </head>
  <body>
    <h1>$title</h1>
    $index
    $documentBodies
  </body>
</html>"""
  }
}

def onePage = new OnePage(selector: selector)
def cb = new CrawlerBuilder()
cb.onDocument { onePage.addDocument(it)  } filterLinks { it.startsWith(base) } worker(worker)
def crawler = cb.build()
def pendigResources = crawler.start([seed as String], new MilliSecondsTimeout(timeout))
println "pendigResources: $pendigResources"
println "write to file: $outfile"
new File(outfile).text = onePage.toString()
println "DONE"
