#pragma once
#include "TextureMapper.h"
#include "TextureMapperJava.h"

namespace WebCore {

class TextureMapperJavaAdapter final : public TextureMapper {
public:
    TextureMapperJavaAdapter()
        :  m_javaMapper(TextureMapperJava::create()) { }

    TextureMapperJava& javaMapper() { return m_javaMapper.get(); }

    void drawBorder(const Color& color, float borderWidth,
                    const FloatRect& rect, const TransformationMatrix& transform)
    {
        m_javaMapper->drawBorder(color, borderWidth, rect, transform);
    }

    void clearColor(const Color& color)
    {
        m_javaMapper->clearColor(color);
    }

    GraphicsContext* graphicsContext()
    {
        return m_javaMapper->graphicsContext();
    }

    void setGraphicsContext(GraphicsContext* context)
    {
            m_javaMapper->setGraphicsContext(context);
    }

private:
    Ref<TextureMapperJava> m_javaMapper;
};

} // namespace WebCore
