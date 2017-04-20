@(gist: gitbucket.gist.model.Gist,
  repositoryUrl: gitbucket.gist.util.GistUtils.GistRepositoryURL,
  revision: String,
  files: Seq[(String, String)]
  )(implicit context: gitbucket.core.controller.Context)
@import gitbucket.core.view.helpers
jqueryScript = document.createElement('script');
jqueryScript.src = '@helpers.assets("/vendors/jquery/jquery-1.12.2.min.js")';
document.head.appendChild(jqueryScript);
prettifyScript = document.createElement('script');
prettifyScript.src = '@helpers.assets("/vendors/google-code-prettify/prettify.js")';
prettifyScript.onload = function() { prettyPrint(); $('pre:has(> span.pln)').hide(); };
document.head.appendChild(prettifyScript);
fireScript = document.createElement('script');
fireScript.setAttribute('type','text/javascript');
fireScript.text = '$(document).load(function() { prettyPrint(); });'

var _html = (function () {/*4f85e035-2513-453b-b435-33f0a12b2339
  <div class="content-wrapper main-center">
    <div class="content body">
      <div style="overflow: hidden;">
        <div style="margin-bottom: 10px;">
          @gist.description
        </div>
        @files.map { case (fileName, content) =>
          <div class="panel panel-default">
            <div class="panel-heading strong" style="padding: 6px; line-height: 30px;">
              @fileName
              <div class="pull-right">
                <a href="@context.path/gist/@gist.userName/@gist.repositoryName/raw/@revision/@fileName" class="btn btn-sm btn-default">Raw</a>
            </div>
            </div>
            @if(helpers.isRenderable(fileName)){
              <div class="panel-body markdown-body" style="padding-left: 16px; padding-right: 16px;">
                @helpers.renderMarkup(List(fileName), content, "master", gist.toRepositoryInfo, false, false, true)
              </div>
            } else {
              <div class="panel-body">
                <pre class="prettyprint linenums blob">@content.toString.replaceAll("<","&lt;").replaceAll(">","&gt;")<pre>
              </div>
            }
          </div>
        }
      </div>
    </div>
  </div>
4f85e035-2513-453b-b435-33f0a12b2339*/}).toString().replace(/(\n)/g, '').split('4f85e035-2513-453b-b435-33f0a12b2339')[1];

document.write('<link href="@helpers.assets("/vendors/bootstrap-3.3.6/css/bootstrap.css")" rel="stylesheet">');
document.write('<link href="@helpers.assets("/vendors/google-code-prettify/prettify.css")" rel="stylesheet">');
document.write('<link href="@context.path/plugin-assets/gist/style.css" rel="stylesheet">');
document.write('<link href="@helpers.assets("/common/css/gitbucket.css")" rel="stylesheet">');
document.write('<link href="@helpers.assets("/vendors/AdminLTE-2.3.8/css/AdminLTE.min.css")" rel="stylesheet">');
document.write(_html.replace(/\\r\\n/g,"\n").replace(/\\/g,""));
