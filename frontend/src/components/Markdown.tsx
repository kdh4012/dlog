import ReactMarkdown from 'react-markdown'
import remarkBreaks from 'remark-breaks'
import remarkCjkFriendly from 'remark-cjk-friendly'
import remarkGfm from 'remark-gfm'

/**
 * 마크다운 본문. 엔터 한 번도 그대로 줄바꿈으로 보여주고(remark-breaks),
 * '**굵게**와' 처럼 한글이 바로 붙어도 강조가 풀리지 않게 한다(remark-cjk-friendly).
 */
export default function Markdown({ children }: { children: string }) {
  return (
    <div className="markdown">
      <ReactMarkdown remarkPlugins={[remarkGfm, remarkCjkFriendly, remarkBreaks]}>{children}</ReactMarkdown>
    </div>
  )
}
