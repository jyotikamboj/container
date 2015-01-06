from django.utils.deprecation import RemovedInDjango20Warning
from django.test import TestCase, ignore_warnings, override_settings


@override_settings(
    STATIC_URL='/path/to/static/media/',
    ROOT_URLCONF='shortcuts.urls',
)
class ShortcutTests(TestCase):

    def test_render_to_response(self):
        response = self.client.get('/render_to_response/')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.content, b'FOO.BAR..\n')
        self.assertEqual(response['Content-Type'], 'text/html; charset=utf-8')

    @ignore_warnings(category=RemovedInDjango20Warning)
    def test_render_to_response_with_request_context(self):
        response = self.client.get('/render_to_response/request_context/')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.content, b'FOO.BAR../path/to/static/media/\n')
        self.assertEqual(response['Content-Type'], 'text/html; charset=utf-8')

    def test_render_to_response_with_content_type(self):
        response = self.client.get('/render_to_response/content_type/')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.content, b'FOO.BAR..\n')
        self.assertEqual(response['Content-Type'], 'application/x-rendertest')

    @ignore_warnings(category=RemovedInDjango20Warning)
    def test_render_to_response_with_dirs(self):
        response = self.client.get('/render_to_response/dirs/')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.content, b'spam eggs\n')
        self.assertEqual(response['Content-Type'], 'text/html; charset=utf-8')

    @ignore_warnings(category=RemovedInDjango20Warning)
    def test_render_to_response_with_context_instance_misuse(self):
        """
        For backwards-compatibility, ensure that it's possible to pass a
        RequestContext instance in the dictionary argument instead of the
        context_instance argument.
        """
        response = self.client.get('/render_to_response/context_instance_misuse/')
        self.assertContains(response, 'context processor output')

    def test_render(self):
        response = self.client.get('/render/')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.content, b'FOO.BAR../path/to/static/media/\n')
        self.assertEqual(response['Content-Type'], 'text/html; charset=utf-8')
        self.assertFalse(hasattr(response.context.request, 'current_app'))

    @ignore_warnings(category=RemovedInDjango20Warning)
    def test_render_with_base_context(self):
        response = self.client.get('/render/base_context/')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.content, b'FOO.BAR..\n')
        self.assertEqual(response['Content-Type'], 'text/html; charset=utf-8')

    def test_render_with_content_type(self):
        response = self.client.get('/render/content_type/')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.content, b'FOO.BAR../path/to/static/media/\n')
        self.assertEqual(response['Content-Type'], 'application/x-rendertest')

    def test_render_with_status(self):
        response = self.client.get('/render/status/')
        self.assertEqual(response.status_code, 403)
        self.assertEqual(response.content, b'FOO.BAR../path/to/static/media/\n')

    @ignore_warnings(category=RemovedInDjango20Warning)
    def test_render_with_current_app(self):
        response = self.client.get('/render/current_app/')
        self.assertEqual(response.context.request.current_app, "foobar_app")

    @ignore_warnings(category=RemovedInDjango20Warning)
    def test_render_with_dirs(self):
        response = self.client.get('/render/dirs/')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.content, b'spam eggs\n')
        self.assertEqual(response['Content-Type'], 'text/html; charset=utf-8')

    @ignore_warnings(category=RemovedInDjango20Warning)
    def test_render_with_current_app_conflict(self):
        with self.assertRaises(ValueError):
            self.client.get('/render/current_app_conflict/')
